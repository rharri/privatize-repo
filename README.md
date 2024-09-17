Privatize GitHub Repositories
=============================

> I needed to privatize several repositories and thought it would be more fun to write a program instead of using GitHub's UI.

### About
A simple utility that iterates through a user's public (un-forked) repositories and sets each one to private. 
If the user specifies repositories in the `exclude.txt` file, those repositories are not set to **"private"**.

>**It bears mentioning that this action has 
[consequences](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/setting-repository-visibility) 
and should be taken with careful consideration. Use of this Software is at your own risk.**

This program is written in Java, however this could also have been accomplished with `curl`, `Python`, or your 
favourite programming language. This program illustrates working with single-file source code programs with Java 
(introduced with Java SE 11).

### Requirements
* Java SE 21*
* FasterXML Jackson library >= 2.18.0-rc1 (included in `lib` directory)
* GitHub Account and Token

**May work with Java SE 11+, however this has not been tested*

### Example Usage
Do a dry run
```bash
% java -cp "lib/*" Main.java <username> <token> --dry-run
```
Run the program against the *real* repositories
```bash
% java -cp "lib/*" Main.java <username> <token>
```

### GitHub Personal Access Token
> **Be careful with your GitHub Personal Access Token; treat it as a password!**
* [Authenticating to the REST API](https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api?apiVersion=2022-11-28)
* [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
* [Keeping your API credentials secure](https://docs.github.com/en/rest/authentication/keeping-your-api-credentials-secure?apiVersion=2022-11-28)