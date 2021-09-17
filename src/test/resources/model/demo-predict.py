import getopt
import sys


def predict(dirs):
    print('dirs: ', dirs)
    return 0.9  # 指标


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['dirs='])
    for opt, arg in opts:
        print(opt, arg[1:-1].split(','))
        if opt == '--dirs':
            dirs = arg[1:-1].split(',')
            print(opt, dirs)
            predict(arg)
    # demo-predict.py --file=123.txt
