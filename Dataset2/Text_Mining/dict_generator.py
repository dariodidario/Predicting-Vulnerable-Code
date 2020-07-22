import ast
import os
'''
This script collect all possible words contained in several text mining java files.
Perform a visit inside every Repository mining folder and for each text mining file 
create a dictionary that contains the different words.
Finally, return a big dictionary containing the words of java file.
'''

def main():
	dict_file_name="text_mining_dict.txt"
	tm_dict ={}
	os.chdir("..")
	cwd = os.getcwd()
	repo_name = "RepositoryMining"
	os.chdir(cwd+"/mining_results")
	for count in range(1,36,1):
		repo = repo_name + str(count)
		if repo != ".DS_Store":
			os.chdir(repo)
			for cvd_id in os.listdir():
				if cvd_id not in [".DS_Store", "CHECK.txt", "ERRORS.txt"]:
					print(cvd_id)
					os.chdir(cvd_id)
					print(os.listdir())
					for folder in os.listdir():
						if folder != ".DS_Store":
							os.chdir(folder)
							for file in os.listdir():
								if file != ".DS_Store":
									if "text_mining.txt" in file:
										read_txt=open(file,"r")
										mini_dict= ast.literal_eval(read_txt.read())
										tm_dict={**tm_dict,**mini_dict}
								else:
									print(".DS_Store occured")
							os.chdir("..")
					os.chdir("..")
			os.chdir("..")
	dict_file=open(dict_file_name,"w+")
	dict_file.write(str(tm_dict))
	dict_file.close()
	print("BUILD SUCCESS !")
	count = 0
	for key in tm_dict:
		count+=1
	print("Ci sono :" + str(count) + " chiavi(che) !")

if __name__ == '__main__':
	main()
