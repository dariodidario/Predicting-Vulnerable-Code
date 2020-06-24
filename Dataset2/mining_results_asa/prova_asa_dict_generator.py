import ast
import os
import csv


def main():
	dict_file_name="ASA_dict.csv"
	tm_dict ={}
	cwd = os.getcwd()
	repo_name = "RepositoryMining" #name of the csv export file of SonarQube
	mini_dict_app= {}
	for count in range(1,36,1):
		repo = repo_name + str(count)
		if repo != ".DS_Store": #check for excluding metadata
			
			for file in os.listdir():
				if file != ".DS_Store":
					if repo in file:
						read_txt=open(file,"r")
						#mini_repo=read_txt.read()
						#mini_dict= ast.literal_eval(mini_repo)
						
						mini_dict_list = []
						first_line=True
						for line in read_txt:
							if(first_line):
								attr_list=line.split("\t")
								for attr in attr_list:
									mini_dict_app[attr]=1
								#print(mini_dict_app.keys())
								first_line=False
							else:
								dict_app={}
								value_list=line.split("\t")
								if(value_list[10]=="VULNERABILITY"):
									dict_app["type"]=value_list[10]
									dict_app["rule"]=value_list[5]
									list_app=value_list[12].split("/")
									dict_app["component"]=list_app[3]+"/"+list_app[4]
									if(count<18):
										dict_app["class"]="pos"
									else:
										dict_app["class"]="neg"
									print(value_list[10])
									mini_dict_list.append(dict_app)
							#mini_dict_list.append(line)
							#tm_dict={**tm_dict,**mini_dict}
				else:
					print(".DS_Store occured")
						
	dict_file=open(dict_file_name,"w+")
	dict_file.write(str(mini_dict_list))
	dict_file.close()
	print("BUILD SUCCESS !")
	count = 0
	for key in tm_dict:
		count+=1
	print("Ci sono :" + str(count) + " chiavi(che) !")

if __name__ == '__main__':
	main()
