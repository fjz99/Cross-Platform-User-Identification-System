import getopt
import sys

LOG_OUTPUT_DIR = "E:/123.txt"
f = open(LOG_OUTPUT_DIR, 'w')


class PredictBase:

    def __init__(self, dirs: list):
        self.dirs = dirs

    def train_one_step(self):
        pass

    def train(self):
        for i in range(MAX_STEP):
            self.train_one_step()
            print((i + 1) / MAX_STEP * 100)
            print((i + 1) / MAX_STEP * 100, file=f)
            sys.stdout.flush()
            f.flush()
            # time.sleep(1)
        print('done')


# class YourPredictModel(PredictBase):
#     """
#     dirs 示例 [E:/abc,D:/cfv]
#     """
#
#     def __init__(self, dirs: list):
#         super().__init__(dirs)
#
#     # 修改这个，如果什么也不做就直接return
#     def train_one_step(self):
#         pass


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['dirs='])
    for opt, arg in opts:
        # print(opt, arg[1:-1].split(','))
        if opt == '--dirs':
            dirs = arg[1:-1].split(',')
            # print(opt, dirs)
            PredictBase(dirs).train()
