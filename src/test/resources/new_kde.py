
import getopt
import numpy as np
import math
import os
import pandas as pd
import json
import re
import time
import sys

def _print(s):
    print(s)
    sys.stdout.flush()


data = {
    "time" : " ",
    "name" : "calculation",
    "dataset": [],
    "output" : {

    }
}

class Location:
    def __init__(self, latitude, longitude):
        self.latitude = latitude
        self.longitude = longitude

    def __sub__(self, other):
        # 转成弧度制
        x1, y1, x2, y2 = map(math.radians, [other.latitude, other.longitude, self.latitude, self.longitude])

        earth_radius = 6378137.0
        temp = math.sin((x1 - x2) / 2) ** 2 + math.cos(x1) * math.cos(x2) * math.sin((y1 - y2) / 2) ** 2
        distance = 2 * earth_radius * math.asin(math.sqrt(temp))

        return distance


def kh(l1, l2, h):
    return (1 / (2 * math.pi * h)) * math.exp(-math.pow(l1 - l2, 2) / (2 * h * h))


def similarity(location_list_1, location_list_2, h):
    n, m = len(location_list_1), len(location_list_2)
    ans = 0

    for li in location_list_1:
        temp = 0
        for lj in location_list_2:
            temp += kh(Location(li['latitude'], li['longitude']), Location(lj['latitude'], lj['longitude']), h)
        ans += temp / m

    return ans/n


#  number_name的字典，用来映射
def To_dict():
    data = []
    with open("number_name.txt", "r", encoding="utf-8") as f_input:
        for line in f_input:
            data.append(list(line.strip().split('#')))
    dataset=pd.DataFrame(data,columns=["number","name","k"])
    d1 = dataset[["number","name"]].set_index("number").to_dict()["name"]
    return d1

# dict_name为number_name的字典
dict_name = To_dict()




def change1(filename , uset):
    filename = re.sub("\D", "", filename)
    filename = str(dict_name.get(filename))
    # filename = filename + "_" + str(dict_name.get(filename))
    # array = np.array(uset[:5])
    array = np.array(uset)
    x = np.matrix.tolist(array)
    for i in range(len(x)):
        x[i][0] = re.sub("\D", "", x[i][0])
        x[i][0] = str(dict_name.get(x[i][0]))
        # x[i][0] = x[i][0] + "_" + str(dict_name.get(x[i][0]))
    return filename , x



def kde(platform_a, platform_b, h=300):



    '读取匹配的jason文件'
    with open("record.json", "r") as file:
        f = file.read()
        datacome = json.loads(f)
        output = datacome['output']
        data["dataset"] = datacome["dataset"]
    # print(output)
    _print("40")

    # 计算时间
    time_begin = time.time()

    _print("50")

    pair = dict()
    k = 0
    'hash过滤后匹配对'
    for fb in output:
        # user_1 = platform_a.get(fb)
        user_1 = fb
        tw_form = output[fb]

        outcome = []

        for tw in tw_form:
            '另一个平台的匹配对'
            tw_number = tw[0]
            user_2 = tw_number
            # user_2 = platform_b.get(tw_number)
            result ={"user":str(dict_name.get(user_2)),"similarity":similarity(platform_a[user_1], platform_b[user_2], h)}
            outcome.append(result)
            # pair[user_1, user_2] = similarity(platform_a[user_1], platform_b[user_2], h)
            # print(user_1, user_2, pair[user_1, user_2])
        # outcome.sort()
        pair.update({str(dict_name.get(fb)):outcome})

    _print("60")

    data["output"] = pair

    time_end = time.time()
    usetime = time_end - time_begin
    data["time"] = usetime


    data_json = json.dumps(data, indent=2, sort_keys=True)

    _print("70")



    # pair = dict()
    # k = 0
    # for user_1 in platform_a:
    #     for user_2 in platform_b:
    #         pair[user_1, user_2] = similarity(platform_a[user_1], platform_b[user_2], h)
    #         print(user_1, user_2, pair[user_1, user_2])
    _print("80")


    # f = open('similarity_new.txt', 'w')
    # json.dump(data_json,f)
    # f.close()
    _print("90")
    _print("100")
    _print("done")
    print(data_json)

def run():
    opts, args = getopt.getopt(sys.argv[1:], '', ['dirs='])
    for opt, arg in opts:
    #     _print(opt, arg[1:-1].split(','), file=f)
        if opt == '--dirs':
            dirs = arg[1:-1].split(',')

    # home = '../inputs'

    platform_a = dirs[0]
    platform_b =dirs[1]

    'platform_b'
    plat_a = {}
    plat_b = {}

    dirs_a = os.listdir(platform_a)
    dirs_b = os.listdir(platform_b)

    _print("10")

    'a平台数据处理'
    for filename in dirs_a:
        i = 0
        with open(platform_a + '/' + filename) as file:
            location = []
            for f in file.readlines():
                filename = re.sub("\D", "", filename)
                i = i + 1
                jd = f.split(' ')[1].replace("'", "")
                wd = f.split(' ')[2].replace("'", "")
                location.append({"user_id": filename, "latitude": float(jd), "longitude": float(wd), "location_id": i})
        plat_a.update({filename: location})

    _print("20")

    'b平台数据处理'
    for filename in dirs_b:
        j = 0
        with open(platform_b + '/' + filename) as file:
            location = []
            for f in file.readlines():
                filename = re.sub("\D", "", filename)
                j = j + 1
                jd = f.split(' ')[1].replace("'", "")
                wd = f.split(' ')[2].replace("'", "")
                location.append({"user_id": filename, "latitude": float(jd), "longitude": float(wd), "location_id": j})
        plat_b.update({filename: location})

    _print("30")

    kde(plat_a,plat_b)



if __name__ == '__main__':
    run()