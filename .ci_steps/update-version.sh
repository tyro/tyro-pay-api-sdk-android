#!/bin/sh
REPO="https://$GITHUB_ACTOR:$TOKEN@github.com/$GITHUB_REPOSITORY.git"
OLD_VERSION="$1"
NEW_VERSION="$2"
VB_VERSION=$(./gradlew -q printVersionName)
VERSION_FILE=./gradle.properties

sed -i "s/\(version *= *['\"]*\)${VB_VERSION}\(['\"]*\)/\1${NEW_VERSION}\2/" ${VERSION_FILE}

git config user.name "${GITHUB_ACTOR}"
git config user.email "${GITHUB_ACTOR}@users.noreply.github.com"
git add $VERSION_FILE
git commit -m "Bump version from $OLD_VERSION to $NEW_VERSION [skip ci]"
git push $REPO

# check version is bumped
CURRENT_VERSION=$(./gradlew -q printVersionName)
echo "CURRENT_VERSION=${CURRENT_VERSION}" >> $GITHUB_OUTPUT
