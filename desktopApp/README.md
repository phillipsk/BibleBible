
# Desktop App Packaging Script

This project includes a shell script (`package_desktop.sh`) to package a desktop application as a signed `.pkg` file for macOS using `jpackage`. It automates signing, verifying the signature, and checking the fingerprint for validation.

## Prerequisites

Before using the script, ensure you have the following:

1. **macOS System**:
    - The script is designed for macOS.

2. **Installed Dependencies**:
    - Java Development Kit (JDK) 17+ with `jpackage`.
    - `pkgutil` (macOS utility for verifying package signatures).
    - `productsign` (macOS utility for signing `.pkg` files).

3. **Valid Developer Certificate**:
    - You need a valid Apple Developer Certificate for signing packages:
        - `3rd Party Mac Developer Installer` or similar.


---

## Setting Up the Environment

### 1. Install Dependencies

Install dependencies if not already installed:

- **JDK 17+**:
    - Install the Amazon Corretto JDK 17 or another JDK 17+ distribution:
      ```bash
      brew install --cask corretto
      ```
    - Verify the installation:
      ```bash
      java -version
      ```

- **Apple Developer Tools**:
    - Install the Xcode command-line tools if not already installed:
      ```bash
      xcode-select --install
      ```

### 2. Set Environment Variables

Export the following variables to your shell environment (`~/.zshrc`, `~/.bash_profile`, etc.), or set them manually in the terminal before running the script.

```bash
# Path to your JDK bin directory
export JDK_PATH="/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home/bin"

# Your macOS Developer Certificate
export MAC_DEVELOPER_ID="3rd Party Mac Developer Installer: Your Name (YourID)"

# Expected SHA256 fingerprint of your signing certificate
export EXPECTED_FINGERPRINT="5B:EC:0D:5A:BA:38:39:80:9E:C4:0C:6C:E1:EE:71:50:AB:E0:96:B1:B3:6B:E3:6F:64:95:14:A4:A9:B4:4D:6A"
```

After editing, reload your shell configuration:
```bash
source ~/.zshrc
```

You can also export these variables temporarily in your terminal session:
```bash
export JDK_PATH="/path/to/jdk/bin"
export MAC_DEVELOPER_ID="Your Certificate Name"
export EXPECTED_FINGERPRINT="Your Certificate Fingerprint"
```

---

## Usage

### Run the Script

1. Ensure the script is executable:
   ```bash
   chmod +x package_desktop.sh
   ```

2. Run the script:
   ```bash
   ./package_desktop.sh
   ```

---

## Script Details

### What the Script Does

1. **Dependency Check**:
    - Ensures that `jpackage`, `pkgutil`, and `productsign` are available.

2. **Packaging the Application**:
    - Uses `jpackage` to create a `.pkg` file with:
        - Application binaries.
        - App metadata, including license and entitlements.

3. **Signing the Package**:
    - Signs the package using the specified macOS Developer Certificate.

4. **Verifying the Signature**:
    - Runs `pkgutil` to verify the signature.
    - Compares the extracted SHA256 fingerprint with the expected value.

5. **Output**:
    - A signed `.pkg` file in the `build/compose/binaries/main/pkg` directory.

### Script Variables

The following variables control the script:

| Variable              | Description                                                | Default Value                                         |
|-----------------------|------------------------------------------------------------|-----------------------------------------------------|
| `JDK_PATH`            | Path to the JDK `bin` directory containing `jpackage`.     | `/Library/Java/JavaVirtualMachines/corretto-17/...` |
| `MAC_DEVELOPER_ID`    | Developer Certificate identity for signing.                | `SetYourDeveloperIDHere`                            |
| `EXPECTED_FINGERPRINT`| Expected SHA256 fingerprint of the signing certificate.    | `SetExpectedFingerprintHere`                        |
| `APP_NAME`            | Name of the application.                                   | `BibleBible`                                        |
| `VERSION`             | Application version.                                       | `1.0`                                               |

---

## Troubleshooting

### Common Errors

1. **Error: Could not find `jpackage`**
    - Ensure `JDK_PATH` is set correctly to point to a JDK 17+ installation.
    - Verify `jpackage` is available:
      ```bash
      $JDK_PATH/jpackage --version
      ```

2. **Error: Could not find appropriate signing identity**
    - Verify `MAC_DEVELOPER_ID` is set to a valid certificate name.
    - Check available certificates:
      ```bash
      security find-identity -v -p codesigning
      ```

3. **Fingerprint Mismatch**
    - Ensure `EXPECTED_FINGERPRINT` matches the actual fingerprint of your certificate:
      ```bash
      pkgutil --check-signature path/to/signed.pkg
      ```

---

