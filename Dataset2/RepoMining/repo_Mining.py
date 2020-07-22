import csv
import requests
import os
from pydriller import RepositoryMining
""""
Execution of the repository mining from the initial_Dataset.csv with pyDriller.
This script takes all tha java classes that are declared as Vulnerable or not.
The aim is to take the Before Image of the commit that fixes the vulnerability.
"""

'''
@Param miniDatasetName : name of the mini dataset created from the script divide_Dataset.py
1. Collect the commits in a python dict and call the function StartMiningRepo.
'''
def initialize(miniDatasetName):
    os.chdir("..")
    cwd=os.getcwd()
    repoName = 'RepositoryMining'+str(miniDatasetName)
    os.chdir("Dataset_Divided")
    name_dataset = str(miniDatasetName)+'.csv'
    with open(name_dataset, mode='r') as csv_file:
            os.chdir("..")
            os.chdir("mining_results")
            if repoName not in os.listdir():
                os.mkdir(repoName)
            os.chdir(repoName)
            csv_reader = csv.DictReader(csv_file)
            first = 0
            data = dict()
            i = 0
            for riga in csv_reader:
                data[i]=riga   
                i+=1
            startMiningRepo(data, cwd, repoName)
            os.chdir(cwd)
'''
@Param data: the line that contains the commits.
@Param cwd: the current directory.
@Param repoName: the name of the destination repo folder.
Check the existence of the Repository and the commit with API.
For each commit, takes all the before images and create the java files that are modified with pyDriller.
Create the resulting ERROR, CHECK files that contins the different status of each commit.
'''
def startMiningRepo(data, cwd, repoName):
    statusOK = "OK!\n"
    statusNE = "NOT EXIST COMMIT\n"
    statusNR = "REPO NOT AVAILABLE\n"
    statusVE = "VALUE ERROR! COMMIT HASH NOT EXISTS\n"
    file1 = open("CHECK.txt", "a")
    file2 = open("ERRORS.txt","a")
    j = 0    
    for line in data:
        link=data[line]['repo_url']+'.git'
        #per chiamare la seconda api e controllare che il commit esiste
        link1=data[line]['repo_url']
        commit_id=data[line]['commit_id']
        cve_id=data[line]['cve_id']
        print(link)
        status = ""
        toWrite = "indice: " + str(j+1) + " link repo: " + str(link1) + " status: "
        response = requests.get(link)
        if response:
            response1 = requests.get(link1+"/commit/"+commit_id)
            if response1:
                try:
                    for commit in RepositoryMining(link, commit_id).traverse_commits():
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
                             os.chdir(cwd+"/"+repoName)
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