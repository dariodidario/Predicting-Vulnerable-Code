import os

def initialize(name_datasetUnion, name_datasetTest):
	cwd = os.getcwd()
	f1= open(name_datasetUnion,"r")
	
	os.chdir("..")
	os.chdir("mining_results_asa")
	
	f2 = open(name_datasetTest,"r")
	f3 = open("check_results.txt","w")
	
	f1.readline()
	f2.readline()
	lista_elementi_Test = []
	lista_elementi_Union =[]
	#Test
	count = 0
	lista = []
	for line in f2:
		lista = line.split(",")
		count +=1
		lista_elementi_Test.insert(count, lista[0])
	print("sono stati scritti :" + str(count) + "elementi")
	#Union
	count2 = 0
	lista1 =[]
	for line1 in f1:
		lista1 = line1.split(",")
		count2 += 1
		lista_elementi_Union.insert(count2, lista1[0].replace(".java_", ".java"))
	print("sono stati scritti" + str(count2) + "elementi")



	for union in lista_elementi_Union:
		if(union in lista_elementi_Test):
			lista_elementi_Test.remove(union)

	print("--------------")
	if(len(lista_elementi_Test) == 0):
		print("I file si trovano!")
	#print(lista_elementi_Test)

	
				
def main():
	name_datasetUnion = "union_TM_ASA.csv"
	name_datasetTest = "csv_ASA_final.csv"
	initialize(name_datasetUnion, name_datasetTest)

if __name__ == '__main__':
	main()