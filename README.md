# josh.benchmarking

A library author ought to know the impact that their changes have on their libaries performance, different libraries which tackle the same problem ought to be able to share common benchmarks, and the community at large ought to have insight into the performance of each release so its members can understand whether the philosophy of a library with regard to performance aligns with their own performance needs.

Instead benchmarks are a thing of niche GitHub repos, contentious debate in blog posts, and very often can be found nestled inside `(comment)` blocks. There is a lot of wonderful things to say about REPL-driven development, for example, it gives fast feedback. However, ad-hoc solutions where you have to expend effort to generate value aren't ideal. It would be better to automate benchmarking such that we produce [technical income][ti].

This is a library to help library authors do that automation. It is not itself a 
benchmarking tool, but is scaffolding for keeping track of historical benchmarking 
runs. Instead of running criterium directly benchmark functions are setup like so:

```
(def benchmarks [{:name ::deeplearning/training :benchmark benchmark}])
```

Then to run your benchmarks you will call:

```
lein benchmark
```

THe benchmark will run and as part of running the benchmarks the results will be 
shoved into a database compatible with the `tech.ml.dataset` API. If no configuration 
is provided then a human readable file format will be used instead.

On future runs the datasets of past runs will be used to give insight into performance 
by showing local relative performance - how things have changed since the last few commits - and global relative performance - how things have changed relative to all entries. 

These results are emitted as events which you can react to either indiviudally or at the end of all benchmarks running. These event handlers are your opportunity to inject your own idea of what should happen in response to performance changes into your library - for example, by failing a CI/CD pipeline if there is a notable regression.

I encourage you to make read access to your performance dataset publicly available via a 
link on your GitHub page. I also encourage you to adopt benchmarks that you fail - for example, many libraries fail miserably when presented with more data than can fit in memory, but others don't.

## Usage

FIXME: Use this for user-level plugins:

Put `[josh.benchmarking "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your `:user`
profile.

FIXME: Use this for project-level plugins:

Put `[josh.benchmarking "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

FIXME: and add an example usage that actually makes sense:

    $ lein benchmark

## License

Copyright © 2022 Joshua Cole

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

# References 

[ti]: https://joshuacol.es/2020/03/06/modeling-technical-income.html
