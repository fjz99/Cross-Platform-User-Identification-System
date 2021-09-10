import getopt
import sys


def train(file):
    print('ok!')


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['file='])
    for opt, arg in opts:
        print(opt, arg)
        if opt == '--file':
            train(arg)
    # demo-predict.py --file=123.txt
