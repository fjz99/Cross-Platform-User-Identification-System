#encoding:utf-8
import os
import re
import time
import math
import matplotlib.pyplot as plt # 
import numpy as np 
import heapq
import operator
from scipy.stats import norm
import gc
import sys as sys
import pandas as pd
import json
import shutil
import random
import getopt

'''
 The copyright is reserved by Wenqiang He.
'''
UR=[] #the user-record list, the ith row is the ith user and the jth column represents the jth record of this user. 
Nt=[]   # a list which records the number of records of each user
users={} #a dictionary that maps the name of the user(username) to its sequence number(uid) which points to the row of UR
buckets={} #a dictionary which maps the bucket ID(bnum) to the bucket's sequence number(bid) in Buckets
Buckets=[] #a list of dictionaries, each dictionary is a bucket whose key is a user name and values are the number and ID numbers of the user's records that fall into this bucket
Bt=[]   # a list which records the number of users of each bucket
Cset=[] #to record the top CK neighbors and their similarities for each user: a list of dictionaries, each dictionary is a candidate set of a specific user whose one key is a neighbor name and value is the similarity
csets={} #to maps the filename to its id in Cset
Fcset=[] #to record the top K final candidates their similarities of each user, a list of dictionaries, each dictionary is a candidate set of a specific user whose one key is a candidate name and value is the similarity
fcsets={} #to maps the filename to its id in Fcset
M=15#4#6#8  #8  #11 #5 #12 #from the 25th bit to the 37bit of each geohash code
H=4 #the first h bits of a time code (year-month-day)
H2=8
N=20 #25  #23#19 #25#the distance error is about 5.5km
Ti=12 #the length of time code to the precision of month
Tj=2**(M+N) # the threshold of time
e=0.5 #to avoid the denominator to be too small
CK=70  #600  #300#200  #100#20
K=50 #50
#S=2**(M+N)
Users=[]  #to record the user name/ID
IDF={} #the key is the bname and the value is the idf
FN=5  #the times to create offsets
Para=[]  #the list to record the parameters of the folder of the documents after preprocessing,[NUSER,NFB,FBN,TWN]
PK=10  #the users that with points less than PK is filtered
S=[] #to record the value of 2**(-n)
#Sb=[]
Sb=[4.0,0.125,0.125,0.125,0,0.125,0.125,0.125,0] #the spatial attention weight
G={} #the key is gcode^g and the value is its number of ones 
Mcset=[] #each element is a trajectory vector dictionary for a user whose key is the bucket id and whose value is the result of geocodes
mcset={} #a dictionary that maps each user to its id in Mcset
TD=[] #the temporal attention weight
df = pd.DataFrame()
data = {
    'time': ' ',
    'name': 'hash',
    'dataset': ' ',
    'output': ' ',
    'other': {


    },
    'sucess': 'True'
}

#to read the data
def readUR(srcpath):

    _print('10')

    if not os.path.exists(srcpath):			
        print("\n****Error:srcpath doesn't exists, you need a srcpath")			  
        raise IOError
        return False

    dir_list=os.listdir(srcpath)
    dir_list.sort()
    tle=dir_list[0][:3]
    dir_list=preprocess(srcpath,dir_list,tle)

    i=0
    for filename in dir_list:
        # *************************************修改
        # print("the processed user is: ", filename)
        users[filename]=i
        infile=os.path.join(srcpath+'/',filename)
        records=readData(infile)
        UR.append(records)
        Nt.append(len(records))
        #print("the number of the records is: ",len(records))
        i+=1
    
    #return True
    return dir_list

#read records of each user
def readData(infile):
    records=[]
    eachfile=open(infile,'r')
    for line in eachfile.readlines():#read each record
        record=extract(line)
        records.append(record)

    eachfile.close()
    return records


#extract bid , time code, geocode and the bucode of each record
def extract(line):
    geocode=0
    timcode=0
    bid=''
    record=[]
    for z in line:
        if z == '@':
            j=line.index(z)
            
        if z == '$':
            i=line.index(z)
            timcode=int(line[j+1:j+1+Ti],2)
            bid+=bin_to_hex(line[i+1:i+1+N])
            #bid+=line[i+1:i+1+N]
            geocode=int(line[i+1:i+1+N+M],2)
            record.append(bid)
            record.append(timcode)
            record.append(geocode)
            record.append(int(line[i+1:i+1+N],2))
            break
    return record #record=[bid,timcode,geocode,bucode]

#to transform the binary number to its hexadecimal form
def bin_to_hex(bnum):
    s=int(bnum,2) #bin to dec
    return hex(s) #dec to hex

#preprocess: to filter users whose record  number is small.
def preprocess(srcpath,dlist,tle):
    flist=[] #to record the preprocessed users 
    Template=np.load(srcpath+"_template.npy",allow_pickle = True).item()
    nuser=0 #number of users
    nfb=0 #number of users from one social site
    ntw=0 #number of users from another social site
    fbn=0 #number of true positives
    
    for filename in dlist:
        infile=os.path.join(srcpath+'/',filename)
        eachfile=open(infile,'r')
        n=len(eachfile.readlines())
        if n < PK:
            continue
        nuser+=1
        flist.append(filename)
        title=filename[:3]
        fid=filename[3:]
        if title == tle:
            nfb+=1
        else:
            ntw+=1
            tname=tle+fid
            flag=tname in flist 
            if flag: # if its true match exists
                if Template[filename]==tname:
                    fbn+=1 
                if Template[tname]==filename:
                    fbn+=1

                
    
    Para.append(nuser)
    Para.append(fbn)
    Para.append(nfb)
    Para.append(ntw)
    
    #to initialize some parameters
    for i in range(N+1):
       
        if i<=N:
            TD.append(1/(i+e))
        

    return flist
    
    
    
        
#construct Buckets based on UR
def buildBuckets(dir_list):

    _print("40")

    #*************************修改****************************
    #print("**********************************The buckets start building!**********************************")
    for filename in dir_list:
        #print("the processed user is: ", filename)
        uid=users[filename]  #to get the uid of the user
        records=UR[uid] # to get the records of this user
        i=0   #to record the sequence number of the record
        Mset={} #to construct the trajectory vector of each user
        kid=len(mcset)
        mcset[filename]=kid
        

        for record in records:
            bnum=record[0]
            flag=bnum in buckets #judge whether the bucket exists
            bn=len(Bt)
            
            if flag:
                bid=buckets[bnum]
                flag2=filename in Buckets[bid] #judge whether the user exists
                if flag2:
                    Buckets[bid][filename][0]+=1   #the number of points of this user that falls into the bucket plus 1
                    Buckets[bid][filename][1].append(i)  #the recordnums records the sequence number of this record
                    Mset[bnum][0]|=record[2]
                    #Mset[bnum][2]+=1
                    Mset[bnum][2]|=record[1]
                else:
                    records=readRecord(i)
                    Buckets[bid][filename]=records
                    Bt[bid]+=1
                    g=[] #g=[geocode,bucode,tcode]
                    g.append(record[2])
                    g.append(record[3])
                    g.append(record[1])
                    #g.append(1)
                    Mset[bnum]=g

            else:#this record is the first one put into this bucket
                buckets[bnum]=bn
                bucket={}
                records=readRecord(i)
                bucket[filename]=records
                Buckets.append(bucket)
                Bt.append(1)
                g=[]
                g.append(record[2])
                g.append(record[3])
                g.append(record[1])
                #g.append(1)
                Mset[bnum]=g
                # ******************************************修改
                # print("the built bucket is: ",bnum)

            i+=1

        Mcset.append(Mset)
                

    #UR.clear()
    #print("UR IS:",len(UR))
    #gc.collect()
    return True

#build a new value in the bucket dictionary
def readRecord(n):
    records=[]
    records.append(1)   # append the number of records
    recordnums=[]
    recordnums.append(n)    #append the sequence number of the record
    records.append(recordnums)
    return records  # records=[numpoint,recordnums]



#find candidates of each user with linear searching/the first filtering layer
def findCan():

    _print("60")

    bnames=list(buckets.keys())#search each bucket
    bnames.sort()
    for bname in bnames:
        bid=buckets[bname]
        bucket=Buckets[bid]
        TF={} #to record tf value of each user in this bucket
        keys=list(bucket.keys())#search each user
        nuser=Bt[bid]  #the number of users in this bucket
        idf=math.log(Para[0]/nuser,2)
        IDF[bname]=idf
        keys.sort()

        #to generate the TF dictionary
        for key in keys:

            np=bucket[key][0]  #the number of records of this user in this bucket
            uid=users[key]
            nt=Nt[uid] #the total number of records of this user
            tf=np/nt
            TF[key]=tf
            kid=mcset[key]
            Mcset[kid][bname].append(tf*idf) #[geocode,bcode,tcode,tf*idf]


        #to deal with each user
        for key in keys:
            candidates,similarity=searchCan(key,TF,idf)
            kflag=key in csets #this user has a neighborhood.
            if kflag:
                addcandidate(key,candidates,similarity)
            else:
                buildCanset(key,candidates,similarity)
        
            
        
        # *******************************************修改
        # print("The No.",bid," bucket has been searched and So are its neighbors!")
  
    return True

##find candidates of each user in this bucket and calculate the statistical similarities
def searchCan(key,TF,idf):
    tf1=TF[key]
    candidates=[] #to record the neighbors
    similarity=[] #to reoord the statistical similarities
    us=TF.keys()
    tle=key[:3]  #title of this candidate
    for f in us:
        tfe=f[:3]
        if tle==tfe: # not add the user of the same platform to the candidate set
            continue
        tf2=TF[f]
        sim=idf*(tf1*tf2)**0.5 #the statistical similarity
        candidates.append(f)
        similarity.append(sim)


    return candidates,similarity

#add the users in this bucket to the candidate set/neighborhood
def addcandidate(key,candidates,similarity):
    uid=csets[key]
    tle=key[:3]
    for i in range(len(candidates)):
        f=candidates[i]

        tfe=f[:3]
        if tle==tfe:
            continue

        flag=f in Cset[uid]  #judge whether this user has existed in this candidate set
        if flag:
            Cset[uid][f][0]+=similarity[i]
            Cset[uid][f][1]+=1
        else:
            g=[]  #to record the similarity and number of shared buckets
            g.append(similarity[i])
            g.append(1)
            Cset[uid][f]=g#similarity[i]

#build a new candidate set/neighborhood and add the users in this bucket to the candidate set/neighborhood
def buildCanset(key,candidates,similarity):
    n=len(csets)
    csets[key]=n  #get the id of the user's candidate set
    cset={}
    tle=key[:3]
    for i in range(len(candidates)):
        f=candidates[i]
        tfe=f[:3]
        if tle==tfe:
            continue
        g=[]  #to record the similarity and number of shared buckets
        g.append(similarity[i])
        g.append(1)
        cset[f]=g#similarity[i]

    Cset.append(cset)


#find top CK candidates
def findCK():

    _print("80")

    names=csets.keys()
    Ncan=[]
    for name in names:
        uid=csets[name]
        cset=Cset[uid]
        Ncan.append(len(list(cset.keys())))

        for j in cset.keys(): #to sum(sim)/n
            cset[j]=cset[j][0]#/cset[j][1]

        b=sorted(cset.items(),key = operator.itemgetter(1),reverse = True) #ranked by the similarity

        n=0
      
        for pair in b:
            if n >= CK:
                del cset[pair[0]]
            n+=1

        
    return sum(Ncan)/(Para[2]*Para[3]*2)#len(Ncan) # the average number of candidates before filtering
        

#find the final nearest K neighbors of each user
def findFinalCan():
    names=list(csets.keys())
    names.sort()
        
    for name in names:
        uid=csets[name]
        keys=list(Cset[uid].keys()) #get the list of candidates
        keys.sort()
        searchFCan(keys,name,uid)
        print("The user: ",name," has find its final candidates!")

    return True

def searchFCan(keys,name,uid):
    nid=mcset[name]
    #userid=csets[name]
    vectors=list(Mcset[nid].keys())
    n=len(vectors)
    vectors.sort()
    tle=name[:3]
    fcset={}
    for key in keys:#for each candidate
        tfe=key[:3]
        if tle==tfe:
            continue
        sim=0
        kmid=mcset[key]
        
        for vec in vectors:#for each bucket of the target user
            vector=Mcset[nid][vec] #the user's vector for this bucket
            rsim=calKDE(vector,kmid) 
            sim+=rsim

        sim/=n
        fcset[key]=sim
    
    findK(fcset,K)
    m=len(fcsets)
    fcsets[name]=m
    Fcset.append(fcset)

            
#to calcualte the KDE
def calKDE(vector,uid):
    rsim=0
    gcode=vector[0]
    bcode=vector[1]
    tcode=vector[2]
    td=vector[3]
    #idf=vector[3]
    bucs=list(Mcset[uid].keys())
    bucs.sort()
    n=1#len(bucs)
    loss=0
    ss=2**(M+N)
    #records=UR[uid]
    ts=1
    for buc in bucs: #for each record of the candidate user that falls in this bucket
       
        vec=Mcset[uid][buc]
        t=vec[2]
        s3=countOne(tcode^t)
        ts=TD[s3]
        g=vec[0]
        b=vec[1]
        s2=abs(bcode-b)
        if s2>H2:
            continue
        if s2==4:
            continue
        if s2==8:
            continue
        n+=1
        s1=gcode^g 
        rsim+=Sb[s2]*(ss-s1)*(td*vec[3])**0.5
 
    return rsim #rsim*n/len(bucs)#rsim/n


def countOne(n):

    if n in G:
        return G[n]

    count = 0  

    while n > 0:

        if n!= 0:

            n = n &(n - 1)

        count += 1

    G[n]=count
    return count
      
            
    
def findK(fcset,L):
 
    b=sorted(fcset.items(),key = operator.itemgetter(1),reverse = True)
    n=0
    for pair in b:
       if n > L or pair[1] <= 0:

           del fcset[pair[0]]

       n+=1


'''
以下为新函数，目的为调整输入和输出
'''

'''
哈希编码文件预处理
'''

def encode1(lat, lon, precision=0.001):
    lat_range, lon_range = [-90.0, 90.0], [-180.0, 180.0]
    geohash = []  # 地理位置哈希表
    codej = []
    codew = []
    code = []
    j = 0
    c_range1 = 1
    c_range2 = 1
    while (c_range1) > precision:
        # print(code, lat_range, lon_range, geohash)
        j += 1

        lon_mid = sum(lon_range) / 2
        # 经度 -> 经度和纬度交叉放置
        if lon <= lon_mid:  # 交线位置给左下
            codej.append(0)
            lon_range[1] = lon_mid
        else:
            codej.append(1)
            lon_range[0] = lon_mid
        c_range1 = lon_range[1]-lon_range[0]
    while (c_range2) > precision:
        # 纬度
        lat_mid = sum(lat_range) / 2
        if lat <= lat_mid:  # 交线位置给左下
            codew.append(0)
            lat_range[1] = lat_mid
        else:
            codew.append(1)
            lat_range[0] = lat_mid
        c_range2 = lat_range[1]-lat_range[0]
    i = 0
    #print(len(codew),len(codej))
    while i<len(codej) or i<len(codew):
        if i< len(codej):
            code.append(codej[i])
        if i< len(codew):
            code.append(codew[i])
        i+=1
    #print(len(code))
    return code
#时间编码（月、日、时、分）
def encode2(time, precision=1):
    """
    :param time: 时间
    :param precision: 精度
    :return:
    """
    time_range = [0, 60.0]
    code = []
    j = 0
    c_time = 10
    while (c_time) > precision:
        j += 1
        time_mid = sum(time_range) / 2
        if time <= time_mid:
            code.append(0)
            time_range[1] = time_mid
        else:
            code.append(1)
            time_range[0] = time_mid
        c_time =time_range[1]-time_range[0]
    return code
#时间编码（年）
def encode3(time, precision=1):
    """
    :param time: 时间
    :param precision: 精度
    :return:
    """
    time_range = [0, 20.0]
    code = []
    j = 0
    c_time = 10
    while (c_time) > precision:
        j += 1
        time_mid = sum(time_range) / 2
        if time <= time_mid:
            code.append(0)
            time_range[1] = time_mid
        else:
            code.append(1)
            time_range[0] = time_mid
        c_time =time_range[1]-time_range[0]
    return code
#时间编码
def encode_time(time):
    code = []
    time_minutes = time%100
    time =int(time/100)
    time_hour = time%100
    time =int(time/100)
    time_day =time%100
    time =int(time/100)
    time_month = time%100
    time =int(time/100)
    time_year =time
    # print(time_year,time_month,time_day,time_hour,time_minutes)
    for c in encode3(time_year - 2000):
        code.append(c)
    for c in encode2(time_month):
        code.append(c)
    for c in encode2(time_day):
        code.append(c)
    for c in encode2(time_hour):
        code.append(c)
    for c in encode2(time_minutes):
        code.append(c)
    # print(len(code))
    return code
#哈希编码
def Hash_code(filename):
    f = filename
    df = pd.read_csv(f,names=['time','jd','wd'],sep=" ")
    t = df['time']
    time_code = []
    gis_code = []
    #经纬度为数字
    # jd = df['jd']
    # wd= df['wd']
    #如果经纬度为字符串
    jdd = df['jd']
    wdd = df['wd']
    jd = []
    wd = []
    for i in jdd:
        jd.append(float(i[1:-1]))
    for i in wdd:
        wd.append(float(i[1:-1]))
    for i in t :
        code = encode_time(i)
        code = [str(i) for i in code]
        time_code.append((''.join(code)).replace("''",""))
    j=0
    while j < len(jd):
        code = encode1(wd[j], jd[j])
        code = [str(i) for i in code]
        gis_code.append((''.join(code)).replace("''",""))
        j+=1
    df['t_code'] =time_code
    df["t_code"] = '@'+df['t_code']
    df['g_code'] =gis_code
    df["g_code"] = '$'+df['g_code']
    # print(df)
    df.to_csv(f, index=False,header=None,sep=" ")

#哈希编码函数接口
def Hash(srcpath):
    dir_list=os.listdir(srcpath)
    dir_list.sort()
    for filename in dir_list:
        Hash_code(srcpath+'/'+filename)

'''
哈希编码文件预处理
'''


#***新建一个DataFrame
def creatDF(dirs,c_filename ,c_array):
    #os.path.basename()用来获取数据集的名字 base1为第一个数据集的名字  base2为第二个数据集的名字
    base1 = os.path.basename(dirs[0])
    base2 = os.path.basename(dirs[1])
    global df
    df=df.append(pd.DataFrame({base1: c_filename, base2: [c_array]}))

def writetofile(df):
    df.to_csv("./1.txt")

#            修改用户名（只取数据集名称）
def change1(filename , uset):
    filename = re.sub("\D", "", filename)
    # array = np.array(uset[:5])
    array = np.array(uset)
    x = np.matrix.tolist(array)
    for i in range(len(x)):
        x[i][0] = re.sub("\D", "", x[i][0])
    return filename , x


#***以jason格式打印（强迫症版）
def printoutcome(path,time):
    srcpath = path[-4:]  #取路径末尾的文件名
    global df
    #DataFrame 转化成字典
    d1 = df[[srcpath[:2], srcpath[2:4]]].set_index(srcpath[:2]).to_dict()[srcpath[2:4]]
    data = {
        'time': time,
        'name': 'hash',
        'dataset': [srcpath],
        'output': d1,
        'other':{
            'k': 5
        },
        'sucess':'True'
    }
    print('{')
    for key in data:
        if key == "output":
            print('"' + str(key) + '": ' + "{")
            for key1 in data.get('output'):
                print('"' + str(key1) + '": ', data.get('output')[key1])
            print("}")
        else:
            print('"' + str(key) + '": ' + str(data[key]))
    print('}')

#***以jason格式打印
def to_jason(path,dirs):
    srcpath = os.path.basename(path)  #取路径末尾的文件名
    global df
    global data
    base1 = os.path.basename(dirs[0])
    base2 = os.path.basename(dirs[1])
    #DataFrame 转化成字典
    d1 = df[[base1, base2]].set_index(base1).to_dict()[base2]
    x={'dataset':[base1,base2],'output':d1}
    data.update(x)

#***合并文件夹
def copy_file(dirs):
    old_path1 = dirs[0]
    old_path2 = dirs[1]
    new_path = dirs[0] + os.path.basename(dirs[1])
    filelist1 = os.listdir(old_path1)  # 列出该目录下的所有文件,listdir返回的文件列表是不包含路径的。
    filelist2 = os.listdir(old_path2)
    if not os.path.exists(new_path):
        os.makedirs(new_path)
    for file in filelist1:
        src = os.path.join(old_path1, file)
        dst = os.path.join(new_path, file)
        shutil.copy(src, dst)
    for file in filelist2:
        src = os.path.join(old_path2, file)
        dst = os.path.join(new_path, file)
        shutil.copy(src, dst)
    return new_path

#生成匹配的模板文件
def to_template(srcpath):
    dir_list = os.listdir(srcpath)
    dir_list.sort()
    length=len(dir_list)
    template ={}
    for i in dir_list:
        for j in dir_list:
            if j.split("_")[1] in i and j != i:
                template.update({i:j})
    for i in dir_list:
        j= random.choice([k for k in dir_list if k.split("_")[0]not in i])
        if i not in template and j.split("_")[0] not in i :
            template.update({i:j})
    template = sorted(template.items(), key=lambda d: d[0], reverse=False)
    template = dict(template)
    # print(template)
    np.save(srcpath+'_template.npy',template)


def _print(s):
    print(s)
    sys.stdout.flush()
'''
以上为新函数，目的为调整输入和输出
'''

def calDis(srcpath,dir_list,dirs):
    global data
    _print("90")
    tempfilename = os.path.basename(srcpath) #用来运行新函数
    Template=np.load(srcpath+"_template.npy",allow_pickle = True).item()
    nm=0 #to record the number of users that has no records
    s=0 #the number of samples that are matched correctly/true positives
    fn=0 #the number of false negatives
    fp=0 #the number of false positive
    ncan=[] #to record the users that have no matches
    ican=[] #the index of matched candidate for each user pair to calculate the AUC
    Ncan=[]  #to record the candidate number of each user  
    Unum=[]   #to record the number of each candidate number value

    for filename in dir_list:
        #****************************修改***********************
        # print("the processed user is: ", filename)
        flag=filename in csets
        if not flag:
            nm+=1
            ncan.append(filename)
            continue

        uid=csets[filename]
        uset=sorted(Cset[uid].items(),key = operator.itemgetter(1),reverse = True)#become a list
        #uset=Cset[uid] #to get its dictionaries
        nt=len(uset)


        # print("the top k candidates : " , uset[:5])

        '''
        新函数切入点
        '''
        #os.path.basename()用来获取数据集的名字 dirs[0]为第一个数据集的名称
        if filename.split('_')[0] in os.path.basename(dirs[0]):        #os.path.basename(dirs[0])
            c_filename , xlist = change1(filename ,uset)
            creatDF(dirs,c_filename, xlist)

        #*******************修改***********************************
        # print("the number of candidates of this user is: ", nt)


        Users.append(filename)
        Ncan.append(nt)
        
        target=Template[filename]

        flag=target[3:]==filename[3:]

        it=0

        if flag:#if it is a positive sample

            flag2=False

            for pair in uset:

                key=pair[0]

                if key==target: 

                    s+=1

                    ican.append(it)

                    flag2=True

                    # print("the matched is:",pair,"\nand its rank is:",it)

                    break

                it+=1



            if not flag2:
                # print("the user ",filename," failed to find its match!")
                fn+=1

        else:
            for pair in uset:
                key=pair[0]
                if key == target:
                   fp+=1

    # writetofile(df)
    _print("100")
    print('done')
    # print(df)
    sncan=sum(Ncan)         
    ave=sncan/len(Ncan)
    # print("the mean value of candidate numbers is: ", ave)
    mnum=max(Ncan)
    # print("the max value of candidate numbers is: ", mnum)
    for i in range(mnum+1):
        k=0
        for j in range(len(Ncan)):
            if Ncan[j] == i:
                k+=1
            

        Unum.append(k)#to get the distribution of candidate numbers

    mode=Unum.index(max(Unum)) # the mode of the list 
    nican=len(ican)
    icancounts=np.bincount(ican)
    ican_mode=np.argmax(icancounts)


    '''
    print("the mode of candidate numbers is: ", mode,"\n and the number of users with the mode candidate number is: ",Unum[mode])
    print("the nm is: ", nm,"\nthe users with no candidate set are:",ncan)
    print("the number of 0 is: ", Unum[0])
    print("the total user number, the true pair number, and the two platform user numbers are respectively: ", Para)
    print("the number of users who find their matches is: ", s)
    print("the average ranking positions of the user pairs is:",np.mean(ican))
    print("the max ranking position of the user pairs is:",max(ican))
    print("the median ranking positions of the user pairs is:",np.median(ican))
    print("the mode ranking positions of the user pairs is:",ican_mode,"\nand the number of matched pairs with the mode ranking position is: ",icancounts[ican_mode]) 
    '''

    '''
    Num2=np.linspace(1,nican,num=nican)        
    plt.title("The distribution of the ranking position of the matched candidate for each user pair in their candidate sets ")    
    plt.scatter(Num2,ican)    
    plt.xlabel("Sequence number of each user")    
    plt.ylabel("Ranking position of its matched candidate")    
    plt.show()
    '''

    recall=s/(s+fn)
    precision=s/(s+fp)  #the precision
    # print("the final precision is: ", precision)
    F1=2*recall*precision/(recall+precision)
    # print("the final F1 score is: ", F1)
    accuracy=(s+Para[0]-Para[1]-fp)/Para[0]
    # print("the final accuracy is: ", accuracy)
    # print("the bucket number is: ", len(Bt))
    ratio=Unum[0]/Para[0]

    x = {
        'max value of candidate numbers ': mnum,
        'mean value of candidate numbers' : ave,
        'the final precision is: ': precision,
        'the final F1 score ':F1,
        'the final accuracy ':accuracy,
        'users with no candidate': ncan,
        'the total user number, the true pair number, and the two platform user numbers are respectively ': Para,
        'number of users who find their matches ': s,
        'max ranking position of the user pairs ': max(ican),
    }

    data['other'].update(x)


    #print("the ratio of users that have candidates is: ", 1-ratio,"\nthe number of false positives are:",fp)
    return recall,sncan/(Para[2]*Para[3]*2) #sncan/(Para[2]*Para[3]*2) RCA




def calDis2(srcpath,dir_list):
    Template=np.load(srcpath+"_template.npy",allow_pickle = True).item()
    s=0 #the number of samples that are matched correctly
    fp=0 #the number of false positive
    fn=0 #the number of false negative
    ncan=[] #record the number of candidates of each user
    unum=[] #record the number of each candidate number
    ican=[] #the index of matched candidate for each user pair
    ican2=[] #the index of false positive for each false pair
    outfile1="method11final2_"+srcpath+"_N_"+str(N)+"_PK_"+str(PK)+"_CK_"+str(CK)+"_K_"+str(K)+"_fmatchfind.txt" #record the true positives and its rank in the candidates
    outfile2="method11final2_"+srcpath+"_N_"+str(N)+"_PK_"+str(PK)+"_CK_"+str(CK)+"_K_"+str(K)+"_falneg.txt" #to record the false positives
    output1=open(outfile1,'a')
    output2=open(outfile2,'a')
    #sumn=0 #to calculate the mean
    title1=srcpath[:2]
    title2=srcpath[2:4]
    for filename in dir_list:
        print("the processed user is: ", filename)
        uid=fcsets[filename]
        #uset=Fcset[uid] #to get its dictionaries
        uset=sorted(Fcset[uid].items(),key = operator.itemgetter(1),reverse = True)#become a list
        nt=len(uset)
        
        print("the number of final candidates of this user is: ", nt)
        ncan.append(nt)
        target=Template[filename]

        flag=target[3:]==filename[3:]
        
        it=0 #the index of the list
        

        if flag:#if it is a positive sample
            flag2=False
            for pair in uset:
                key=pair[0]
                if key==target: 
                    s+=1
                    ican.append(it)
                    flag2=True
                    print("the matched is:",pair,"\nand its rank is:",it)
                    output1.write(filename+" "+key+" "+str(pair[1])+" "+str(it)+" "+str(nt)+"\n")
                    break
                it+=1
            if not flag2:

                fn+=1
        else:

            for pair in uset:

                key=pair[0]

                if key == target:
                    fp+=1
                    print("the false positive is:",pair,"\nand its rank is:",it)
                    output2.write(filename+" "+key+" "+str(pair[1])+" "+str(it)+" "+str(nt)+"\n")
                    ican2.append(it)
                    break

                it+=1

    output1.close()
    output2.close()
    sncan=sum(ncan)
    ave=sncan/len(ncan)
    print("the mean value of final candidate numbers is: ", ave)
    mnum=max(ncan)
    print("the max value of final candidate numbers is: ", mnum)
    for i in range(mnum+1):
        k=0
        for j in range(len(ncan)):
            if ncan[j] == i:
                k+=1
            

        unum.append(k)
    mode=unum.index(max(unum)) # the mode of the list 
   
    #Unum = np.bincount(Ncan)
    #mode=np.argmax(Unum)  #calculate the mode
    nican=len(ican)
    icancounts=np.bincount(ican)
    ican_mode=np.argmax(icancounts)
    print("the mode of final candidate numbers is: ", mode,"\nand the number of users with the mode candidate number is: ",unum[mode])
    print("the number of 0 is: ", unum[0])
    print("the number of users who find their matches in final candidates is: ", s)
    print("the average ranking positions of the user pairs is:",np.mean(ican))
    print("the max ranking position of the user pairs is:",max(ican))
    print("the median ranking positions of the user pairs is:",np.median(ican))
    print("the mode ranking positions of the user pairs is:",ican_mode,"\nand the number of matched pairs with the mode ranking position is: ",icancounts[ican_mode])
 
    if len(ican2)>0:
        print("the max ranking position of false positives is:",max(ican2))
        print("the average ranking positions of false positives is:",np.mean(ican2))
        print("the median ranking positions of false positives is:",np.median(ican2))

    '''
    Num2=np.linspace(1,nican,num=nican)        
    plt.title("The distribution of the ranking position of the matched candidate for each user pair in their candidate sets ")  
    plt.scatter(Num2,ican)    
    plt.xlabel("Sequence number of each user")    
    plt.ylabel("Ranking position of its matched candidate")    
    plt.show()
    '''

    precision=s/(s+fp)  #the precision
    print("the final precision is: ", precision,"\nthe number of false positives are:",fp)
    napcp=Para[2]*Para[3]*2  #num_all_possible_can_pairs
    accuracy=(s+Para[0]-Para[1]-fp)/Para[0]
    print("the final accuracy is: ", accuracy)
    recall=s/(s+fn)#Para[1]
    print("the total user number, the true pair number, and the two platform user numbers are respectively: ", Para)
    F1=2*recall*precision/(recall+precision)
    print("the final F1 score is: ", F1)
    
    return recall,sncan/napcp#s/(Para[2]*Para[3]*2)




if __name__ == '__main__':
    opts, args = getopt.getopt(sys.argv[1:], '', ['dirs='])
    for opt, arg in opts:
        # print(opt, arg[1:-1].split(','), file=f)
        if opt == '--dirs':
            dirs = arg[1:-1].split(',')

    # dirs=['E:/fb','E:/fs']  #调试用

    tempfile = copy_file(dirs)    #合并dirs所指数据集的文件夹

    Hash(tempfile)

    to_template(tempfile)

    inpath1=tempfile #("Please input a srcpath:" input)     #读取数据集文件    程序接口**************

    flist=readUR(inpath1)

    _print("20")

    if(len(flist)):
        time_begin = time.time()  #to record the time

        _print("30")

        #print('Congratulations!The user files have been all read!')
        if (buildBuckets(flist)):

            _print("50")

            #print('Congratulations!The bucket files have been all built!')
            if(findCan()):

                _print("70")

                #print('Congratulations!All users have found their candidates!')
                rca=findCK()
                recall,ratio=calDis(inpath1,flist,dirs) #传dirs只是为了获取数据集名称
                #把召回率rca和召回率recall添加到data['other']中
                data['other'].update({'rca':rca ,'recall':recall})
                #*******************************修改**********************************
                # print('***************************\nthe rca is:',rca)
                #print('the recall of this candidate set is: ', recall)
                time_end = time.time()
                time = time_end - time_begin
                #添加到data中  （时间）
                data['time'] = time
                # print('***************************\nThe running time is :', time)
                # print("***************************\nthe k is  = ", 5)

                '''
                新函数切入点
                '''
                to_jason(inpath1,dirs)
                data_json = json.dumps(data, indent=2, sort_keys=True)
                print(data_json)
                '''
                of = int(input("Please input a number:"))
                if(findFinalCan()):
                    print('Congratulations!All users have found their final candidates!')
                    recall2,ratio2=calDis2(inpath1,flist)
                    print('the 2nd recall of this candidate set is: ', recall2,'\nthe ratio of all filtered candidate pairs in all possible candidate pairs is:',ratio2)
                    time_end = time.time()
                    time = time_end - time_begin
                    print('***************************\nThe running time is :', time)
                else:
                    print("Warning! \n There are some errors in finding final candidates!\n")
                '''
            else:
                print("Warning! \n There are some errors in finding candidates!\n")
        else:
            print("Warning! \n There are some errors in reading buckets!\n")

 
    else:
        print("Warning! \n There are some errors in reading user files!\n")
    #删除合并的临时文件夹
    if os.path.exists(tempfile):
        shutil.rmtree(tempfile)
    #删除模板文件
    if os.path.exists(tempfile+'_template.npy'):
        os.remove(tempfile+'_template.npy')