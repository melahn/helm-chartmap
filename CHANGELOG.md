# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- Improvements in README.md
- More test cases to keep coverage at 100%
- Tidied up the index files of examples
- Updated example charts to use more recent chart versions
- Improve representation of chart types
- Align the values in the keywords and maintainers sets in helm chart representations
- Improve formatting of docker image text
- Set PLANTUML_LIMIT_SIZE for the user, if not already set
- Handle case where there is a chart referenced in dependencies that only exists as a sub-chart
- Updated example-env-spec.yaml to add more values needed by alfresco charts
- Generate a chart map using the refresh option if the non-refresh attempt fails
- Relax version parsing to allow non-SemVer names
- Use 'helm dep update' recursively when -r parameter used
- Handle case where there is a chart referenced in dependencies that only exists as a sub-chart
- Update test chart to eliminate references to outdated helm chart repos
- Remove reference to prebuilt jar
- Bad link in the README.md pointing to more examples of the Java constructor

## [1.1.0] - 2022-03-29

- Security Updates
- Added many more test cases to achieve 100% test coverage
- CI/CD now with GitHub Actions for build and deploy
- Added deploy to GitHub Packages as well as Maven Central
- Upgraded to Helm Version 3
- Removed -d option
- Added and updated example charts
- Added script to generate helm chart examples
- Added Sonar Cloud scan
- Now using archive extract to avoid zip slip vulnerability
- Much code grooming to elminate lint errors and reduce cognitive complexity
- Cleaned pom to make it compliant with Apache coding standards

## [1.0.2] - 2020-04-26

### Changed in 1.0.2

- Setup for publishing to maven central

## [1.0.1] - 2020-04-25

### Added

- CHANGELOG.md (this file)
- Badges

### Changed in 1.0.1

- More readable help format

### Fixed

- CVE-2020-10969, CVE-2020-9546, CVE-2020-11620, CVE-2020-10672

## 1.0.0 - 2020-04-21

- Initial version

[Unreleased]: https://github.com/melahn/helm-chartmap/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/melahn/helm-chartmap/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/melahn/helm-chartmap/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/melahn/helm-chartmap/compare/v1.0.0...v1.0.1
