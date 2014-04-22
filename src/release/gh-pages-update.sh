#!/bin/bash

#
# Update gh-pages during a release.
#
# Usage:
#
#   gh-pages-update.sh release.properties
#
# OR
#
#   SCM_URL=/home/dalaro/tinkerelius/faunus-0.4.3 SCM_TAG=0.4.3 gh-pages-update.sh
#
# The first form reads SCM_URL and SCM_TAG from release.properties.
# The second form provides these two variables to the script via the
# environment.  Both of the variables must be set and must have
# nonempty values.  If both the environment variables and a file
# argument are provided, then the environment variables take
# precedence.
#
# This script must be invoked from a git repository root.
#
# Regardless of how SCM_URL and SCM_TAG are provided, the script does
# the following.
#
# * Create gh-pages branch in the local git repo if it doesn't
#   already; it must already exist on the remote/origin end
#
# * Clone SCM_URL into a new repo in CLONE_DIR and cd there
#
# * Fetch and checkout gh-pages in the new clone
#
# * Copy contents of <original-repo-root>/doc/html into
#   wikidoc/$SCM_TAG/ in the new clone
#
# * Copy Maven-generated javadocs from
#   <original-repo-root>/target/apidocs into javadoc/$SCM_TAG/ in the
#   new clone
#
# * Makes wikidoc/current/ mirror wikidoc/$SCM_TAG/ and makes
#   javadoc/$SCM_TAG/ mirror javadoc/$SCM_TAG/ in the new clone
# 
# * Copies target/site-resources/index.html to base of gh-pages.
#
# * Commits all changes in the new clone and pushes them to the
#   original repository; does not push to github

set -e
set -u

declare -r PAGE_BRANCH=gh-pages
declare -r CLONE_DIR=/home/dalaro/tinkerelius/faunus-0.4.3/target/pages
declare -r JAVADOC_DIR=/home/dalaro/tinkerelius/faunus-0.4.3/target/apidocs
declare -r WIKIDOC_DIR=/home/dalaro/tinkerelius/faunus-0.4.3/doc/html
declare -r WIKI_INDEX=/home/dalaro/tinkerelius/faunus-0.4.3/target/release-resources/wikidoc-index.html
declare -r MAIN_INDEX=/home/dalaro/tinkerelius/faunus-0.4.3/target/release-resources/index.html

if [ -z "${SCM_URL:-}" -o -z "${SCM_TAG:-}" ]; then
    if [ -z "${1:-}" ]; then
        echo "Usage: $0 path/to/release.properties"
        exit -1
    else
        # Change directory to the folder containing release.properties,
        # store the full path to that directory, then clone it to $CLONE_DIR.
        pushd "`dirname $1`" >/dev/null
        declare -r SCM_URL="`pwd`"
        popd >/dev/null
        echo Read SCM_URL from $1: $SCM_URL

        # Search release.properties for the current release tag.
        declare -r SCM_TAG=`sed -rn 's|\\\\||g; s|^scm\.tag=||p' $1`
        echo Read SCM_TAG from $1: $SCM_TAG
    fi
else
    echo Read SCM_URL from environment: $SCM_URL
    echo Read SCM_TAG from environment: $SCM_TAG
fi

# If the gh-pages branch doesn't already exist, then create it.
# The branch must exist for the sake of the clone we're about to make.
if [ ! -e .git/refs/heads/"$PAGE_BRANCH" ]; then
    git branch "$PAGE_BRANCH" origin/"$PAGE_BRANCH"
fi

git clone "$SCM_URL" "$CLONE_DIR"
cd "$CLONE_DIR"
git fetch origin refs/remotes/origin/"$PAGE_BRANCH":refs/heads/"$PAGE_BRANCH"
git checkout "$PAGE_BRANCH"
git pull origin "$PAGE_BRANCH"

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
