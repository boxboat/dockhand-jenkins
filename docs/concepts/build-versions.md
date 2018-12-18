# Build Versions Git Repository

The shared library relies on a Git repository defined in the Global configuration for storing data about build versions.

The following information is stored:

- `image-versions`: contains subdirectories for each event with a file for each image version
- `job-triggers`: contains subdirectories for each job with a file for triggers that cause that job to be run
- `repo-versions`: contains subdirectories for each repository with a file for each repository version
