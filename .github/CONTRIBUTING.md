# Contributing to shimmer-android
We want to make contributing to this project as easy and transparent as
possible.

## Our Development Process

Our internal repository, which is copied to GitHub, is our source of truth,
and development happens both directly in GitHub and internally.
Internally, we might build tools around this library that we might move
into the GitHub repository in the future, but we won't fork for internal changes.

This repository has two modules:

* in `shimmer/` you'll find the shimmer library code that provides all the
functionality that you would use in your app.
* in `sample/` you'll find an example app that utilizes the library.

## Development set up

It is important that you first install the snapshot version of the shimmer
library before you can start developing. To do so, you can simply run
`./gradlew shimmer:installArchives`. This will install the snapshot artifact to
 your local repository so you can work on the latest and greatest version.

## Pull Requests
We actively welcome your pull requests.

1. Fork the repo and create your branch from `main`.
2. If you've added code that should be tested, add tests
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. If you haven't already, complete the Contributor License Agreement ("CLA").

## Contributor License Agreement ("CLA")
In order to accept your pull request, we need you to submit a CLA. You only need
to do this once to work on any of Facebook's open source projects.

Complete your CLA here: <https://code.facebook.com/cla>

## Issues
We use GitHub issues to track public bugs. Please ensure your description is
clear and has sufficient instructions to be able to reproduce the issue.

Facebook has a [bounty program](https://www.facebook.com/whitehat/) for the safe
disclosure of security bugs. In those cases, please go through the process
outlined on that page and do not file a public issue.

## Coding Style
* We use Google's Java formatter (https://github.com/google/google-java-format)
with default settings. Please use it to format your changes.

## License
By contributing to shimmer-android, you agree that your contributions will be licensed
under its BSD license.
