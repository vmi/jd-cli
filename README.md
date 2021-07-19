jd-cli
======

Java decompiler CLI using [jd-core](https://github.com/java-decompiler/jd-core).

Usage
-----

```
jd-cli [OPTIONS] TARGET ...

[TARGET]
* class file
* class directory
* jar file

[OPTIONS]
-cp, --classpath CLASSPATH  Specify CLASSPATH.
-o,  --out-dir OUTDIR       Specify the output directory of generated Java source.
                            (default: the same directory as the class file)
```
