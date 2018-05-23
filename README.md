# Jenkins DSL Code

Configuration as code for [Jenkins][jenkins].  This repository contains code to
generate and run jobs for [build.gimp.org][ci].

# Job DSL

Jenkins jobs are tracked in configuration as code in the format of the [Job DSL
Plugin][job-dsl], a Jenkins plugin designed for this sort of thing.

See also [Job DSL API][job-dsl-api] which contains some of the DSL syntax.  It
doesn't contain all of the DSL syntax because the Job DSL plugin dynamically
generates DSL for some Jenkins plugins.

The [`jobs/`](jobs) directory contains the source code for generating all
Jenkins jobs.

# Build Pipeline DSL

Pipeline DSL is the flow of a build process for a Jenkins job.

All GIMP dependencies and GIMP itself must run through the same build pipeline
where generically it copies and extracts dependent artifacts from prior
dependent builds, builds and packages the software, and then collects the
package.  If this explanation isn't quite clear then refer to the following
section _Example of a pipeline_.  With that in mind refer to the [`vars/`](vars)
directory for the build pipeline of GIMP and all of its dependencies.

The `vars/` directory is part of a [Jenkins shared pipeline
library][pipeline-lib].

### Example of a pipeline

The pipeline for [GIMP][gimp] involves the following steps in order:

1. Copy artifacts from [BABL][babl] `master` branch built from source and move
   into `/data`.
2. Copy artifacts from [GEGL][gegl] `master` branch built from source and move
   into `/data`.
3. Copy artifacts from [libmypaint][libmypaint] tag `v1.3.0` built from source
   and move into `/data`.
4. Copy artifacts from [mypaint-brushes][mypaint-brushes] branch `v1.3.x` built
   from source and move into `/data`.
5. Extract all artifacts located at `/data/*.tar.gz`.
6. Build and install GIMP.
7. Package `/home/jenkins/usr/*` as the GIMP artifact and name it
   `gimp-internal.tar.gz`.
8. Collect the `gimp-internal.tar.gz` artifact.

[babl]: http://gegl.org/babl/
[ci]: https://build.gimp.org/
[dhub]: https://hub.docker.com/r/gimp/gimp/
[gegl]: http://gegl.org/
[gimp]: http://www.gimp.org/
[jenkins]: https://jenkins.io/
[job-dsl-api]: https://jenkinsci.github.io/job-dsl-plugin/
[job-dsl]: https://plugins.jenkins.io/job-dsl
[libmypaint]: https://github.com/mypaint/libmypaint
[mypaint-brushes]: https://github.com/Jehan/mypaint-brushes/tree/v1.3.x
[pipeline-lib]: https://jenkins.io/doc/book/pipeline/shared-libraries/
