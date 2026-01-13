# How to Build SecureFileVault

Since you are using IntelliJ IDEA and do not have Maven installed globally in your system path, the easiest way to build the executable JAR file is through the IDE.

## Option 1: Using IntelliJ IDEA (Recommended)

1.  Open the **Maven** sidebar on the right side of the IDE window.
2.  Expand **SecureFileVault** -> **Lifecycle**.
3.  Double-click **package**.
    -   This will compile the code, run tests, and bundle everything into a JAR.
4.  Once completed, check the directory: `target/`.
5.  You will find a file named `filevault-1.0-SNAPSHOT.jar` (or similar).
    -   **Note**: The file `original-filevault-1.0-SNAPSHOT.jar` is the one *without* dependencies. You want the shaded (uber) jar, which is usually the larger one without the "original-" prefix.

## Option 2: Using Command Line (If Maven is installed)

If you decide to install Maven manually in the future:
1.  Open your terminal in the project directory.
2.  Run: `mvn clean package`.

## How to Run the JAR

Once built, you can run the application from the command line:

```powershell
java -jar target/filevault-1.0-SNAPSHOT.jar
```
