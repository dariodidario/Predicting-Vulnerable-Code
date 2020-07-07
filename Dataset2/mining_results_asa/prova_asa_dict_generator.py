import ast
import os
import csv


def main():
	dict_file_name="ASA_dict.csv"
	tm_dict ={}
	cwd = os.getcwd()
	repo_name = "RepositoryMining_ASAResults" #name of the csv export file of SonarQube
	mini_dict_app= {}
	writed_quote=False
	dict_file=open(dict_file_name,"w+")
	dict_file.write("[")
	for count in ["neg","pos"]:
		repo = repo_name
		if repo != ".DS_Store": #check for excluding metadata
			
			for file in os.listdir():
				if file != ".DS_Store":
					if repo+"_"+count in file:
						print(file)
						read_txt=open(file,"r")
						#mini_repo=read_txt.read()
						#mini_dict= ast.literal_eval(mini_repo)
						
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
									if(count=="pos"):
										dict_app["class"]=count
										print(dict_app)
										if(writed_quote):
											dict_file.write(",")
										else:
											writed_quote=True
										dict_file.write(str(dict_app))
									else:
										if(count=="neg"):
											print(count)
											dict_app["class"]=count

											if(writed_quote):
												dict_file.write(",")
											else:
												writed_quote=True
											dict_file.write(str(dict_app))
							#mini_dict_list.append(line)
							#tm_dict={**tm_dict,**mini_dict}
				else:
					print(".DS_Store occured")
	dict_file.write("]")
	dict_file.close()
	print("BUILD SUCCESS !")

if __name__ == '__main__':
	main()
