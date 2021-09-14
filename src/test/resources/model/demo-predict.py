import getopt
import sys


def predict(input):
    print('input: ', input)
    return 0.9  # 指标


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['input='])
    for opt, arg in opts:
        print(opt, arg)
        if opt == '--input':
            predict(arg)
    # demo-predict.py --file=123.txt
