import json
import threading
import time
import queue

from threading import Thread
from tornado.ioloop import IOLoop
from tornado.iostream import StreamClosedError
from tornado.tcpserver import TCPServer


class Model:

    def predict(self, x):
        raise NotImplemented

    """
        必须只训练一次，方便计算百分比
    """

    def trainOneStep(self, files):
        raise NotImplemented

    def train(self, files):
        raise NotImplemented

    def pause(self):
        raise NotImplemented

    def continue0(self):
        raise NotImplemented

    def load(self, path):
        raise NotImplemented

    def getPercentage(self):
        raise NotImplemented


# 没做状态校验
class BaseModel(Model):
    class State:
        TRAINING = 0  # 正在训练中
        PREDICTING = 1  # 正在预测中
        PAUSED = 2
        UNTRAINED = 3  # 模型不行，要么load，要么train
        READY = 4  # 已经load，准备好进行任何操作

    name = None
    checkpointPath = None
    stage = None
    state = State.UNTRAINED
    step = None
    totalSteps = None
    threadLock = threading.Lock()

    def __init__(self, name='unknown', checkpointPath=None, stage=1, step=None, totalSteps=None):
        self.step = step
        self.totalSteps = totalSteps
        self.checkpointPath = checkpointPath
        self.stage = stage
        self.name = name

    def updateState(self, newState: int):
        self.threadLock.acquire(True)
        self.state = newState
        self.threadLock.release()

    def predict(self, x):
        raise NotImplemented

    def trainOneStep(self, files):
        raise NotImplemented

    def train(self, files):
        State = self.State
        self.step = 0
        while True:
            if self.state in [State.PAUSED]:
                time.sleep(0.5)
                continue
            else:
                self.step += 1  # 这里不同步
                self.trainOneStep(files)
        self.updateState(self.State.READY)

    def pause(self):
        self.updateState(self.State.PAUSED)

    def continue0(self):
        self.updateState(self.State.TRAINING)

    def load(self, path):
        if self.state not in [self.State.UNTRAINED, self.State.READY]:
            raise ValueError
        self.load0(path)
        self.updateState(self.State.READY)

    def load0(self, path):
        raise NotImplemented

    def getPercentage(self):
        return self.step / self.totalSteps


# 测试用
class ModelStub(BaseModel):
    pass


# 只有暂停和继续是新开一个线程
# 简便处理，没有session id，一条连接走到底。即一个ModelServer的实例就对应一个model，一条连接
class ModelServer(TCPServer):
    def __init__(self):
        super().__init__()
        self.model = None
        self.mapping = {OperationType.NEW_MODEL: self.process_new_model}
        self.queue = queue.Queue()

    async def handle_stream(self, stream, address):
        while True:
            try:
                data = await stream.read_until(b"\n")
                print('data', data)
                await stream.write(self.process(data))
                print('done')
            except StreamClosedError as e:
                print(e)
                break

    def return_err(self, e: Error, data):
        data['args'] = {
            "err": str(e)
        }
        return

    def process(self, data) -> bytes:
        try:
            data = json.load(data)
            type = data['args']
            if self.model is None:
                self.model = self.process_new_model(data['name'])
            # TODO
            Thread(target=self.mapping[data['type']], kwargs=data['args'] + {'queue': self.queue}).start()

        except Error as e:
            self.return_err(e, data)

    def process_new_model(self, name):
        # TODO model文件扫描规则
        return ModelStub()

    def process_predict(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_train(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_stop(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_pause(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_continue(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_load(self, queue: queue.Queue):
        queue.put(self.model.predict())

    def process_get_percentage(self, queue: queue.Queue):
        queue.put(self.model.predict())


class OperationType:
    PAUSE = 0x0
    CONTINUE = 0x1
    LOAD = 0x2
    STOP = 0x3
    TRAIN = 0x4
    PREDICT = 0x5
    GET_TRAINING_PERCENTAGE = 0x6
    # NEW_MODEL = 0x7
