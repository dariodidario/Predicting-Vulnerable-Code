import os

def initialize(name_csv_mining, name_csv_soft_m, new_Union):
	cwd = os.getcwd()
	#Return to Union folder
	os.chdir("..")
	os.chdir("Union_TM_ASA")
	csv_mining = open(name_csv_mining, "r+")
	#Return to Union
	os.chdir("..")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("Software_Metrics")
	csv_software_metric = open(name_csv_soft_m, "r+")
	os.chdir("..")
	#Return to Total_Combination
	os.chdir("Union/Total_Combination")
	number_of_file = 0
	flag_mining = True
	flag_soft_met = True
	for line_tm in csv_mining:
		#Se è la prima riga del csv del mining
		if(flag_mining == True):
			flag_mining = False
			line_sm = csv_software_metric.readline()
			#Se è la prima riga del csv delle software metrics
			if( flag_soft_met == True):
				flag_soft_met = False
					
				withoutFirst2Argument = line_sm.split(',')
				withoutFirst2Argument = withoutFirst2Argument[2:11]
				toString = ""
				for element in withoutFirst2Argument:
					toString+= "," + element
				withoutClassInMining = line_tm[:-7]
				new_Union.write(withoutClassInMining + toString + ",class")
				new_Union.write("\n")
				#prendi il nome dei file nelle software metrics
				
		else:
			csv_software_metric.seek(0,0)
			csv_software_metric.readline()
			for line_sm in csv_software_metric:
				file_name_sm = line_sm.split(',')[1].replace("\"", "")
				file_name_tm = line_tm.split(',')[0].replace(".java_",".java")
				#print("Software Metric :" + file_name_sm)
				#print("Text Mining :" + file_name_tm)
				if(file_name_tm == file_name_sm):
					number_of_file += 1
					#print("i file sono uguali")
					#ottengo il valore della classe (pos || neg) (TEXT_MINING)
					class_element = getClass(line_tm)
					#ottengo i caratteri (TEXT_MINING)
					element_text_mining = another_option(None, line_tm, class_element)
					#SOFTWARE METRICS
					element_software_metrics = another_option(line_sm, None, class_element)

					
					new_Union.write(element_text_mining + element_software_metrics + class_element)
				#else:
					#print("i file non sono uguali")
	print("i file che sono stati letti e scritti sono :" + str(number_of_file))
	print("BUILD SUCCESS")

'''
@Param "line_sm" : linea del dataset delle software metrics che contiene tutti i valori delle SOFTWARE METRICS
@Param "line_tm" : linea del dataset del text mining che contiene tutti i valori del TEXT MINING 
@Param "class_element" : parametro che descrivere la classe (pos o neg)
è possibile chiamare questa funzione omettendo una delle due linee

SE OMETTIAMO IL TEXT MINING : 

1. se la TextM è omessa allora si effettua lo split degli elememtni dell softw_metric e li si mette in una lista
2. per ogni elemento in lista, si tolgono i possibili \n e si contano gli elementi
3. se sono maggiori di 1 ovvero stiamo tralasciando il tipo del file e il nome del file.
4. si concatenano gli elementi in una stringa e si fa return

SE OMETTIAMO LE SOFTWARE METRICS : 
1. si effettua lo split di tutti gli elementi e si mettono in una lista
2. si rimuove l'elemento che specifica la classe (pos o neg)
3. per ogni elemento in lista si tolgono i possibili \n e si concatenano gli elementi
4. si concatenano gli elementi e si restituiscono
'''
def another_option(line_sm, line_tm, class_element):
	if(line_tm == None):
		toString = ""
		lista = line_sm.split(",")
		#print(lista)
		count = 0
		for element in lista:
			elem = element.replace("\n", "")
			#print(elem)
			if(count > 1):
				toString += elem + ","
			count += 1
		return toString
	elif(line_sm == None):
		toString = ""
		lista = line_tm.split(",")
		lista.remove(class_element)
		for element in lista:
			elem = element.replace("\n", "")
			toString += elem + ","
		return toString

'''
@Param "line" : linea del text mining che vengono splittati e inseriti in una lista 
e si restituisce solo l'ultimo elemento quindi o "pos" o "neg"
'''
def getClass(line):
	lista = line.split(",")
	count = 0
	for element in lista:
		#print("-" + element)
		count+=1
	#print("sono pos" + lista[count-1])
	return lista[count-1]
	

def main():
	new_Union = open("3COMBINATION.csv", "w")
	name_csv_mining = "Union_TM_ASA.csv"
	name_csv_soft_m = "mining_results_sm_final.csv"
	initialize(name_csv_mining, name_csv_soft_m, new_Union)


if __name__ == '__main__':
	main()