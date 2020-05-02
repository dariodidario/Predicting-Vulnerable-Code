import csv
import requests
import os
from pydriller import RepositoryMining

def initialize():
    cwd=os.getcwd()
    with open('papero_2.csv', mode='r') as csv_file:
        if "RepositoryMining" not in os.listdir():
             os.mkdir("RepositoryMining")
        os.chdir("RepositoryMining")
        csv_reader = csv.DictReader(csv_file)
        first = 0
        data = dict()
        i = 0
        for riga in csv_reader:
            data[i]=riga   
            i+=1
        startMiningRepo(data, cwd)

def startMiningRepo(data, cwd):    
    for line in data:
        link=data[line]['repolink']+'.git'
        #per chiamare la seconda api e controllare che il commit esiste
        link1=data[line]['repolink']
        commit_id=data[line]['commit_id']
        cve_id=data[line]['cve_id']
        print(link)
        response = requests.get(link)
        if response:
            response1 = requests.get(link1+"/commit/"+commit_id)
            if response1:
                print("dentro")
                for commit in RepositoryMining(link, commit_id).traverse_commits():
                    print("ciao amico")
                    for mod in commit.modifications:
                       if ".java" in mod.filename:
                             if cve_id not in os.listdir():
                                 os.mkdir(cve_id)
                             os.chdir(cve_id)
                             if commit_id not in os.listdir():
                                os.mkdir(commit_id)
                             os.chdir(commit_id)
                             if mod.source_code_before != None:
                             	javafile=open(mod.filename,"w+")
                             	print(mod.source_code_before)
                             	javafile.write(mod.source_code_before)
                             os.chdir(cwd+"/RepositoryMining")
            else:
                print("Commit non esistente!")       
        else:
            print("Repository non esistente o privata")

def main():
    initialize()
if __name__ == '__main__':
    main()
