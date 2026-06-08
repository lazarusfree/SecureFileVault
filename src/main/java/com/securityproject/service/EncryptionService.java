package com.securityproject.service;

import com.securityproject.config.VaultConfig;
import com.securityproject.exception.CryptoOperationException;
import com.securityproject.exception.FileAccessDeniedException;
import com.securityproject.repository.AuditRepository;
import com.securityproject.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

public class EncryptionService {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptionService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final KeyService keyService;
    private final FileRepository fileRepo;
    private final AuditRepository auditRepo;

    public EncryptionService(KeyService keyService, FileRepository fileRepo, AuditRepository auditRepo) {
        this.keyService = keyService;
        this.fileRepo = fileRepo;
        this.auditRepo = auditRepo;
    }

    public record EncryptionResult(String algorithm, String ciphertextSha256, String saltBase64, String ivBase64,
                                   String authenticatedMetadata) {
    }

    public record DecryptionResult(String algorithm, String authenticatedMetadata) {
    }

    public EncryptionResult encrypt(File inputFile, File outputFile, int ownerUserId) {
        try {
            SecretKey masterKey = keyService.loadOrGenerate();
            byte[] inputBytes = Files.readAllBytes(inputFile.toPath());
            byte[] salt = randomBytes(VaultConfig.SALT_BYTES);
            byte[] iv = randomBytes(VaultConfig.GCM_IV_BYTES);
            SecretKey fileKey = deriveFileKey(masterKey, salt);
            String aad = buildMetadata(ownerUserId, inputFile.getName(), inputBytes.length);
            byte[] aadBytes = aad.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance(VaultConfig.CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, fileKey, new GCMParameterSpec(VaultConfig.GCM_TAG_BITS, iv));
            cipher.updateAAD(aadBytes);
            byte[] ciphertext = cipher.doFinal(inputBytes);

            try (FileOutputStream os = new FileOutputStream(outputFile)) {
                os.write(VaultConfig.FILE_MAGIC);
                os.write(VaultConfig.FILE_VERSION);
                os.write(salt.length);
                os.write(iv.length);
                os.write(ByteBuffer.allocate(Integer.BYTES).putInt(aadBytes.length).array());
                os.write(salt);
                os.write(iv);
                os.write(aadBytes);
                os.write(ciphertext);
            }

            EncryptionResult result = new EncryptionResult(
                    "AES-256-GCM",
                    sha256Hex(ciphertext),
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(iv),
                    aad
            );

            fileRepo.save(ownerUserId, inputFile.getAbsolutePath(), outputFile.getAbsolutePath(),
                    result.algorithm(), result.ciphertextSha256(), result.authenticatedMetadata());
            auditRepo.log(ownerUserId, "File encrypted with " + result.algorithm() + ".");
            LOG.info("File encrypted: {} -> {}", inputFile.getName(), outputFile.getName());
            return result;

        } catch (CryptoOperationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Encryption failed", e);
            throw new CryptoOperationException("Encryption failed: " + e.getMessage(), e);
        }
    }

    public DecryptionResult decrypt(File inputFile, File outputFile, int expectedOwnerUserId) {
        try {
            SecretKey masterKey = keyService.loadOrGenerate();
            byte[] fileBytes = Files.readAllBytes(inputFile.toPath());

            if (!hasCurrentFormat(fileBytes)) {
                return legacyDecrypt(inputFile, outputFile, masterKey);
            }

            ByteBuffer header = ByteBuffer.wrap(fileBytes);
            byte[] magic = new byte[VaultConfig.FILE_MAGIC.length];
            header.get(magic);
            byte version = header.get();
            int saltLength = Byte.toUnsignedInt(header.get());
            int ivLength = Byte.toUnsignedInt(header.get());
            int aadLength = header.getInt();

            if (version != VaultConfig.FILE_VERSION || saltLength <= 0 || ivLength <= 0 || aadLength <= 0) {
                throw new CryptoOperationException("Unsupported or damaged SecureFileVault file header.");
            }

            byte[] salt = new byte[saltLength];
            byte[] iv = new byte[ivLength];
            byte[] aadBytes = new byte[aadLength];
            header.get(salt);
            header.get(iv);
            header.get(aadBytes);
            byte[] ciphertext = Arrays.copyOfRange(fileBytes, header.position(), fileBytes.length);
            String aad = new String(aadBytes, StandardCharsets.UTF_8);

            int fileOwnerId = extractOwnerId(aad);
            if (expectedOwnerUserId >= 0 && fileOwnerId != expectedOwnerUserId) {
                auditRepo.log(expectedOwnerUserId, "Blocked unauthorized decrypt attempt: " + inputFile.getAbsolutePath());
                throw new FileAccessDeniedException("Authenticated file owner does not match the logged-in user.");
            }

            if (!fileRepo.isOwner(expectedOwnerUserId, inputFile.getAbsolutePath())) {
                auditRepo.log(expectedOwnerUserId, "Blocked unauthorized decrypt attempt: " + inputFile.getAbsolutePath());
                throw new FileAccessDeniedException("You cannot decrypt a file you did not encrypt.");
            }

            SecretKey fileKey = deriveFileKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance(VaultConfig.CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, fileKey, new GCMParameterSpec(VaultConfig.GCM_TAG_BITS, iv));
            cipher.updateAAD(aadBytes);
            byte[] plaintext = cipher.doFinal(ciphertext);
            Files.write(outputFile.toPath(), plaintext);

            auditRepo.log(expectedOwnerUserId, "Decrypted file after integrity/authenticity verification: " + inputFile.getName());
            LOG.info("File decrypted: {} -> {}", inputFile.getName(), outputFile.getName());
            return new DecryptionResult("AES-256-GCM", aad);

        } catch (FileAccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Decryption failed", e);
            throw new CryptoOperationException("Decryption failed: " + e.getMessage(), e);
        }
    }

    @Deprecated(forRemoval = true)
    private DecryptionResult legacyDecrypt(File inputFile, File outputFile, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] outputBytes = cipher.doFinal(Files.readAllBytes(inputFile.toPath()));
            Files.write(outputFile.toPath(), outputBytes);
            LOG.warn("Used legacy AES-ECB decryption for: {}", inputFile.getName());
        } catch (Exception e) {
            throw new CryptoOperationException("Legacy decryption failed.", e);
        }
        return new DecryptionResult("AES-ECB legacy", "Legacy file did not include authenticated metadata.");
    }

    public static String sha256Hex(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new CryptoOperationException("SHA-256 hash failed.", e);
        }
    }

    private static SecretKey deriveFileKey(SecretKey masterKey, byte[] salt) {
        try {
            char[] keyMaterial = Base64.getEncoder().encodeToString(masterKey.getEncoded()).toCharArray();
            PBEKeySpec spec = new PBEKeySpec(keyMaterial, salt, VaultConfig.PBKDF2_ITERATIONS, VaultConfig.FILE_KEY_BITS);
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new CryptoOperationException("Key derivation failed.", e);
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String buildMetadata(int ownerUserId, String fileName, long length) {
        return "owner=" + ownerUserId +
                ";name=" + Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8)) +
                ";bytes=" + length +
                ";created=" + Instant.now();
    }

    private static int extractOwnerId(String aad) {
        for (String part : aad.split(";")) {
            if (part.startsWith("owner=")) {
                return Integer.parseInt(part.substring("owner=".length()));
            }
        }
        throw new CryptoOperationException("Authenticated metadata is missing the owner field.");
    }

    private static boolean hasCurrentFormat(byte[] fileBytes) {
        return fileBytes.length > VaultConfig.FILE_MAGIC.length
                && Arrays.equals(Arrays.copyOf(fileBytes, VaultConfig.FILE_MAGIC.length), VaultConfig.FILE_MAGIC);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new CryptoOperationException("SHA-256 computation failed.", e);
        }
    }
}
