import getopt
import sys
import time

LOG_OUTPUT_DIR = "E:/123.txt"
MAX_STEP = 10  # 计算训练百分比用,不训练的话，这个=0
f = open(LOG_OUTPUT_DIR, 'w')


class TrainBase:

    def __init__(self, dirs: list):
        self.dirs = dirs

    def train_one_step(self):
        pass

    def output(self):
        return json.dumps({
            'time': 1000,
            'name': 'demo',
            'dataset': ['aa', 'bb'],
            'output': {
                '1': [[1, 0.1], [2, 0.2]]
            },
            'other': {
                'k': 5
            },
            'success': "true"
        })
    def train(self):
        for i in range(MAX_STEP):
            self.train_one_step()
            print((i + 1) / MAX_STEP * 100)
            print((i + 1) / MAX_STEP * 100, file=f)
            sys.stdout.flush()
            f.flush()
            time.sleep(1)
        print('done')
        print(self.output())
        sys.stdout.flush()


class YourModel(TrainBase):
    """
    dirs 示例 [E:/abc,D:/cfv]
    """

    def __init__(self, dirs: list):
        super().__init__(dirs)

    # 修改这个，如果什么也不做就不写
    def train_one_step(self):
        pass

    # 训练的输出，如果是没有参数的，输出即为匹配的结果、用户筛选结果
    # 如果没有输出，则不要写这个
    # def output(self):
    #     pass


if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['dirs='])
    for opt, arg in opts:
        print(opt, arg[1:-1].split(','), file=f)
        if opt == '--dirs':
            dirs = arg[1:-1].split(',')
            print(opt, dirs, file=f)
            YourModel(dirs).train()
    f.flush()
