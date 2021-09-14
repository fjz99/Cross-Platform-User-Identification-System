import getopt
import sys
import time

f = open("E:/123.txt", 'w')


def train(file):
    for i in range(10):
        print((i+1)*10)
        sys.stdout.flush()
        print(i+1, file=f)
        f.flush()
        time.sleep(1)
    # print('done')


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['file='])
    for opt, arg in opts:
        # print(opt, arg)
        if opt == '--file':
            train(arg)
    print('done')
    # demo-predict.py --file=123.txt
