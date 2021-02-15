# RefDiff Python - Plugin

## Installation

- Clone [RefDiff](https://github.com/aserg-ufmg/RefDiff/)
- Edit `settings.gradle` and include `refdiff-python` at end.
- Run `gradlew eclipse`

## Parser

- Install dependencies

```
pip install -r requirements.txt
```

```
usage: parser.py [-h] -f FILE [-p]

RefDiff parser for Python programming language

optional arguments:
  -h, --help            show this help message and exit
  -f FILE, --file FILE  input file
  -p, --pretty          output json in pretty format
```

Example of usage

```
./parser.py --file example.py --pretty
```
