# AutoPUT
Developed by [Keita Tsukamoto](https://github.com/tsukakei) and [Yuta Maezawa](https://github.com/mzw)

## What's AutoPUT?
AutoPUT is an automated tool for retrofitting your closed unit tests into parameterized unit tests.

## How to Use
### Set up
Clone our repository and compile AutoPUT.


```
 $ cd workspace
 $ git clone https://github.com/web-eng/AutoPUT
 $ cd AutoPUT
 $ mvn compile test-compile dependency:copy-dependencies
```


Then, make `output` and `subjects` directories under the `AutoPUT`.
And, clone subject projects you would like to parameterize unit tests to `subjects/`.

```
 $ cd AutoPUT
 $ mkdir output
 $ mkdir subjects
 $ cd subjects
 $ git clone https://github.com/apache/commons-bcel
``` 

### Run

We provide a script for one-command-line.
You can use AutoPUT with ease as below.

1. For detecting CUTs to be parameterized.

    ```
    $ cd AutoPUT
    $ sh/run ${SUBJECT} detect
    ```
    
1. For generating PUTs corresponding to detected CUTs.

    ```
    $ cd AutoPUT
    $ sh/run ${SUBJECT} generate
    ```

1. For checking generated PUTs satisfy two requirements R1 and R2.

    ```
    $ cd AutoPUT
    $ sh/run ${SUBJECT} requirement
    ```
    
## Publication(s)
_Keita Tsukamoto, Yuta Maezawa and Shinichi Honiden
“AutoPUT: An Automated Technique for Retrofitting Closed Unit Tests into Parameterized Unit Tests”,
In Proceedings of the 33rd ACM/SIGAPP Symposium on Applied Computing (SAC’18)_