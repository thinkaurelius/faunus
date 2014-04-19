#!/bin/bash

#
# Update gh-pages during a release.
#
# Usage: gh-pages-update.sh release.properties
#
# Clones the repo specified by scm.url in release.properties.
# 
# Checks out the gh-pages branch.
#
# Copies javadoc and wikidoc into (java|wiki)doc/scm.tag from
# release.properties}/.
#
# Updates (java|wiki)doc/current/ to be symlinks to new docs.
#
# Copies target/site-resources/index.html to base of gh-pages.
#
# Does not commit or push anything.
#

[ -z "$1" ] && { echo "Usage: $0 path/to/release.properties"; exit -1; }

set -e
set -u

declare -r PAGE_BRANCH=gh-pages
declare -r CLONE_DIR=${project.build.directory}/pages
declare -r JAVADOC_DIR=${project.build.directory}/apidocs
declare -r WIKIDOC_DIR=${project.basedir}/doc/html
declare -r WIKI_INDEX=${project.build.directory}/release-resources/wikidoc-index.html
declare -r MAIN_INDEX=${project.build.directory}/release-resources/index.html

# Search release.properties for the current release tag.
declare -r SCM_TAG=`sed -rn 's|\\\\||g; s|^scm\.tag=||p' $1`

# Change directory to the folder containing release.properties,
# store the full path to that directory, then clone it to $CLONE_DIR.
pushd "`dirname $1`" >/dev/null
declare -r SCM_URL="`pwd`"
popd >/dev/null

echo Set SCM_URL from $1: $SCM_URL
echo Set SCM_TAG from $1: $SCM_TAG

# If the gh-pages branch doesn't already exist, then create it.
# The branch must exist for the sake of the clone we're about to make.
if [ ! -e .git/refs/heads/"$PAGE_BRANCH" ]; then
    git branch "$PAGE_BRANCH" origin/"$PAGE_BRANCH"
fi

git clone "$SCM_URL" "$CLONE_DIR"
cd "$CLONE_DIR"
git fetch origin refs/remotes/origin/"$PAGE_BRANCH":refs/heads/"$PAGE_BRANCH"
git checkout "$PAGE_BRANCH"

echo Copying generated wikidoc to wikidoc/$SCM_TAG
cd "$CLONE_DIR/wikidoc"
cp -a "$WIKIDOC_DIR" "$SCM_TAG"
cp "$WIKI_INDEX" "$SCM_TAG"/index.html
echo Deleting and recreating wikidoc/current for $SCM_TAG
git rm -r current
cp -a "$SCM_TAG" current
echo Adding wikidoc changes to git index
git add "$SCM_TAG" current

echo Copying generated javadoc to javadoc/$SCM_TAG
cd "$CLONE_DIR/javadoc"
cp -a "$JAVADOC_DIR" "$SCM_TAG"
echo Deleting and recreating javadoc/current $SCM_TAG
git rm -r current
cp -a "$SCM_TAG" current
echo Adding javadoc changes to git index
git add "$SCM_TAG" current

echo Copying new gh-pages index.html to root
cd "$CLONE_DIR"
cp "$MAIN_INDEX" index.html
echo Adding new gh-pages index.html file to the git index
git add index.html

git commit -m "Page updates for $SCM_TAG"
git push origin "$PAGE_BRANCH"

cat <<EOF
Locally committed new javadoc, wikidoc, and index.html reflecting release
version $SCM_TAG into gh-pages.

  Clone directory = $CLONE_DIR
  Parent repo     = $SCM_URL
  Branch modified = $PAGE_BRANCH
  Release version = $SCM_TAG

These changes have been pushed to the parent repository.
EOF
