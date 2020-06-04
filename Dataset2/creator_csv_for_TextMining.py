import ast
import os
import random
def main():
	dict_file_name="text_mining_dict.txt"
	final_csv_name = "csv_mining_final.txt"
	cwd = os.getcwd()
	repo_name = "RepositoryMining"
	os.chdir(cwd+"/mining_results")
	final_csv = open(final_csv_name, "w+")
	big_dict_file = open(dict_file_name, "r+")
	big_dict = ast.literal_eval(big_dict_file.read())
	csv_sorted = sorted(big_dict.keys())
	final_csv.write("NameClass")
	for element in csv_sorted:
		final_csv.write(" ," + str(element))
	final_csv.write(" ,class")
	final_csv.write("\n\n")
	for count in range(1,36,1):
		if count !=18:
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
											current_file = open(file, "r")
											#converto il dizionario piccolo in un dic
											current_dict = ast.literal_eval(current_file.read())
											name_of_file_for_csv = file.replace("text_mining.txt", "")
											final_csv.write(name_of_file_for_csv)
											for element in csv_sorted: 
												if element in current_dict.keys():
													final_csv.write(",")
													final_csv.write(str(current_dict[element]))	
												else:		
													final_csv.write(",")		
													final_csv.write(str(0))
											if count < 18:
												final_csv.write(", pos")
											else:
												if count >18:
													final_csv.write(", neg")

												

											final_csv.write("\n")
									else:
										print(".DS_Store occured")
								os.chdir("..")
						os.chdir("..")
				os.chdir("..")
	"""
	count = 0
	for key in tm_dict:
		count+=1
	print("Ci sono :" + str(count) + " chiavi(che) !")
	"""
	final_csv.close()
	print("BUILD SUCCESS !")

if __name__ == '__main__':
	main()
