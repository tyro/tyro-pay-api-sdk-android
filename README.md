# Tyro Pay API SDK (Android)

This SDK supports easy integration of Tyro's Pay API in you Android app.

**Currently the SDK only supports Google Pay, more functionality will be included later**


## Installation

### Requirements
* Android 5.0 (API level 21) and above
* Compile SDK 31 and above
* Android Gradle Plugin 7.4.2
* Gradle 5.4.1+
* AndroidX

### Configuration
Store GitHub Packages Username

`export GITHUB_PACKAGES_USER={YOUR_GITHUB_USERNAME}`

Setup GitHub Personal Access Token (PAT)

* Follow this [GitHub guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic) to generate a PAT with `read:packages` permission
* Store the new token as an environment variable: 
`export GITHUB_PACKAGES_TOKEN={YOUR_GITHUB_TOKEN}`


Add `tyro-pay-api-sdk-android` to your `build.gradle` dependencies.
```
dependencies {
    implementation 'com.tyro:tyro-pay-api-sdk-android:1.0.0'
}
```
Add the repository to your `build.gradle`
```
repositories {
    maven {
            url = uri("https://maven.pkg.github.com/tyro/tyro-pay-api-sdk-android")
            credentials {
                username = System.getenv("GITHUB_PACKAGES_USER")
                password = System.getenv("GITHUB_PACKAGES_TOKEN")
            }
        }
}
```

More detailed guide is available in [GitHub Packages - working with the gradle registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)


## Getting Started

### Instructions
Get started with our [guides](https://docs.connect.tyro.com/app/apis/pay/google-pay-android/#accept-google-pay-on-android-early-preview)

### Example
Our [example app](https://github.com/tyro/tyro-pay-api-google-pay-sdk-android/tree/master/example-app) demonstrates how to use the GooglePay client which integrates our Tyro Pay API.
