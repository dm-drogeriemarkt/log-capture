# Secrets needed for deployment

Secrets are stored in an environment for Github Actions to deploy to the Sonatype OSS repo.

## GPG_KEYNAME

The name of the GPG key used for signing. See `gpg -K` for key names.

## GPG_KEY_BASE64

Base64-encoded key export of the signing key.

```shell
gpg --export-secret-keys ${GPG_KEYNAME} | base64
```

## GPG_PASSPHRASE

Passphrase of the GPG key

## OSSRH_JIRA_USERNAME

User name used for authenticating at the Sonatype OSS repo. Same as the Password used to log in
at https://issues.sonatype.org

## OSSRH_JIRA_PASSWORD

Password used for authenticating at the Sonatype OSS repo.
