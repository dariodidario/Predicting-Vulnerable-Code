import csv
import requests
import os
from pydriller import RepositoryMining

def initialize():
    cwd=os.getcwd()
    with open('dataset100_200.csv', mode='r') as csv_file:
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
    statusOK = "OK!\n"
    statusNE = "NOT EXIST COMMIT\n"
    statusNR = "REPO NOT AVAILABLE\n"
    statusVE = "VALUE ERROR! COMMIT HASH NOT EXISTS\n"
    file1 = open("CHECK.txt", "a")
    file2 = open("ERRORS.txt","w+")
    j = 0    
    for line in data:
        link=data[line]['repolink']+'.git'
        #per chiamare la seconda api e controllare che il commit esiste
        link1=data[line]['repolink']
        commit_id=data[line]['commit_id']
        cve_id=data[line]['cve_id']
        print(link)
        status = ""
        toWrite = "indice: " + str(j+1) + " link repo: " + str(link1) + " status: "
        response = requests.get(link)
        if response:
            response1 = requests.get(link1+"/commit/"+commit_id)
            if response1:
                print("dentro")
                try:
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
                                javafile.write(mod.source_code_before)
                             os.chdir(cwd+"/RepositoryMining")
                    status = statusOK
                    toWrite = toWrite + status
                    file1.write(toWrite)
                    j+=1
                except ValueError:
                    print("ValueError:SHA for commit not defined ")
                    status= statusVE    
                    toWrite = toWrite +status
                    file1.write(toWrite)
                    file2.write(toWrite+","+commit_id)

            else:
                status = statusNE
                toWrite = toWrite + status
                file1.write(toWrite)
                j+=1    
        else:
            status = statusNR
            toWrite = toWrite + status
            file1.write(toWrite)
            j+=1
    file1.close()
    file2.close()
def main():
    initialize()
if __name__ == '__main__':
    main()