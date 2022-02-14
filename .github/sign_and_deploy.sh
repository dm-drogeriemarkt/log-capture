#!/usr/bin/env bash
KEY_FILE='daniel.flassak.open.source.private.key'

echo $GPG_KEY_BASE64 | base64 -d  > ${KEY_FILE}
gpg --passphrase "${GPG_PASSPHRASE}" --batch --yes --fast-import ${KEY_FILE}

if [[ "${REF_TYPE}" == "tag" ]]; then
    # 'install' cannot be used in addition to 'deploy', because that makes the signatures invalid by re-creating jars
    # after they have been signed.
    #
    # So we can **only** call the 'deploy' target here, which is why 'source:jar' and 'javadoc:jar' are called
    # explicitly before 'deploy' so that their artifacts are signed, too.
    #
    # '-P sign' is used here instead of 'gpg:sign', because 'gpg:sign' seemingly has the same effect as 'install'
    # (invalid signatures to to re-created jars)
    #
    # There may be an easier way to sign and deploy all the artifacts (sources, javadoc, binaries and pom), but after
    # four hours of debugging this, I'm satisfied that it works at all.
    mvn --batch-mode -DskipTests=true -Dproject.version=${REF_NAME} -P sign clean source:jar javadoc:jar deploy
    SUCCESS=$?
else
    echo "this should only be run for tags"
    SUCCESS=1
fi

# just to be safe, although this deleting these should not be necessary
rm ${KEY_FILE}
rm -rf ~/.gnupg

exit ${SUCCESS}
