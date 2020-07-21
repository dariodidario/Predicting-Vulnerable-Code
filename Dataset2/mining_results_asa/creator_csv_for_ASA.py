import ast
import os
import random
import json

def main():
	dict_file_name="ASA_dict.csv"
	rules_dict_name="ASA_rules_dict.csv"
	final_csv_name = "csv_ASA_final2.csv"
	cwd = os.getcwd()
	big_dict={}
	repo_name = "RepositoryMining"
	final_csv = open(final_csv_name, "w+")
	rules_file= open(rules_dict_name,"r+")
	rules_dict=json.loads(rules_file.read().replace("\'","\""))
	final_csv.write("Name")
	for key in rules_dict.keys():
		final_csv.write(", "+key)
	final_csv.write(", class")
	final_csv.write("\n")


	print(rules_dict)
	big_dict_file = open(dict_file_name, "r+")
	big_dict_text=big_dict_file.read().replace("\'","\"")
	list_vuln=json.loads(big_dict_text)
	for el in list_vuln:
		if el["component"] in big_dict.keys():
			app=big_dict[el["component"]]
			if el["rule"] in app.keys():
				print(el["rule"])
				big_dict[ el["component"] ] [ el["rule"] ]+=1
			else:
				big_dict[ el["component"] ] [ el["rule"] ]=1
			
		else:
			big_dict[el["component"]]={}
			big_dict[el["component"]]["class"]=el["class"]
			big_dict[el["component"]] [el["rule"]]=1

	for java_class_key in big_dict.keys():
		java_class_dict=big_dict[java_class_key]
		final_csv.write("\n")
		final_csv.write(java_class_key)
		for rule in rules_dict:
			print(rule)
			if rule in big_dict[java_class_key].keys():
				final_csv.write(", "+str(big_dict[java_class_key][rule]))
				print(big_dict[java_class_key][rule])
			else:
				final_csv.write(", 0")
		final_csv.write(", "+str(big_dict[java_class_key]["class"]))
		




	rules_file.close()
	final_csv.close()
	print("BUILD SUCCESS !")

if __name__ == '__main__':
	main()
