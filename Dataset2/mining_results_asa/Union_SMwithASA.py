import os

def initialize(name_csv_sm, name_csv_asa, new_Union):
	cwd = os.getcwd()
	#Return to Dataset2 folder
	os.chdir("..")
	os.chdir("Software_Metrics_Mining_Result")
	csv_sm = open(name_csv_sm, "r+",encoding="utf-8")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("mining_results_asa")
	csv_asa = open(name_csv_asa, "r+",encoding="utf-8")
	number_of_file = 0
	found = False
	flag_sm = True
	flag_asa = True
	for line_sm in csv_sm:
		found=False
		#Se è la prima riga del csv delle software metrics
		if(flag_sm == True):
			flag_sm = False
			line_asa = csv_asa.readline()
			#Se è la prima riga del csv delle asa
			if( flag_asa == True):
				flag_asa = False
					
				withoutFirst2Argument = line_asa.split(',')
				withoutFirst2Argument = withoutFirst2Argument[1:18]
				toString = ""
				for element in withoutFirst2Argument:
					toString+= "," + element
				withoutClassInMining = line_sm[:-7]
				new_Union.write(withoutClassInMining + toString + ",class")
				new_Union.write("\n")

				#prendi il nome dei file nelle asa
				
		else:
			csv_asa.seek(0,0)
			csv_asa.readline()
			#print("Eccomi")
			#ottengo i caratteri (SOFTWARE METRICS)
			file_name_sm = line_sm.split(',')[1].replace("\"","")

			#ottengo il valore della classe (pos || neg) (SOFTWARE METRICS)
			class_element = getClass(line_sm)
			class_element = class_element.replace(" ","")
			element_software_metrics = another_option(None, line_sm, class_element)
			for line_asa in csv_asa:
				file_name_asa = line_asa.split(',')[0].replace("\"", "")
				#print("Eccomi")
				#print("ASA Results :" + file_name_asa)
				#print("Software Metrics :" + file_name_sm)
				if(file_name_sm == file_name_asa):
					number_of_file += 1
					#print("i file sono uguali")
					#SOFTWARE METRICS
					
					#Static Analysis Results
					element_ASA = another_option(line_asa, None, class_element)
					found = True
					#scrivo la tupla del nuovo dataset
					new_Union.write(element_software_metrics + element_ASA + class_element)		
				#else:
					#print("i file non sono uguali")
			if(found==False): #se lo script non trova la classe nel dataset ASA
				element_ASA ="" # inserisce 19 valori uguali a 0
				for i in range(0,18):
					element_ASA +="0,"
				new_Union.write(element_software_metrics +element_ASA + class_element)
	#print("Number:"+str(number_of_file))
	print("i file che sono stati letti e scritti sono :" + str(number_of_file))
	print("BUILD SUCCESS")

'''
@Param "line_asa" : linea del dataset delle asa che contiene tutti i valori delle ASA
@Param "line_sm" : linea del dataset delle software metrics che contiene tutti i valori delle SOFTWARE METRICS 
@Param "class_element" : parametro che descrivere la classe (pos o neg)
è possibile chiamare questa funzione omettendo una delle due linee

SE OMETTIAMO LE SOFTWARE METRICS: 

1. se la Software Metrics è omessa allora si effettua lo split degli elememtni dell ASA e li si mette in una lista
2. per ogni elemento in lista, si tolgono i possibili \n e si contano gli elementi
3. se sono maggiori di 1 ovvero stiamo tralasciando il tipo del file e il nome del file.
4. si concatenano gli elementi in una stringa e si fa return

SE OMETTIAMO LE ASA : 
1. si effettua lo split di tutti gli elementi delle software metrics e si mettono in una lista
2. si rimuove l'elemento che specifica la classe (pos o neg)
3. per ogni elemento in lista si tolgono i possibili \n e si concatenano gli elementi
4. si concatenano gli elementi e si restituiscono
'''
def another_option(line_asa, line_sm, class_element):
	if(line_sm == None):
		toString = ""
		#print("Class_Element:"+class_element+"_fine")
		class_element=class_element.replace(" ","")
		class_element=class_element.replace("\n","")
		#print("Class_Element_AFTER:"+class_element+"_fine")
		lista = line_asa.replace(" ","").replace("\n","").split(",")

		#print(lista)
		lista.remove(class_element)
		#print(lista)
		count = 0
		for element in lista:
			elem = element.replace("\n", "")
			#print(elem)
			if(count > 0):
				toString += elem + ","
			count += 1
		return toString
	elif(line_asa == None):
		toString = ""
		lista = line_sm.split(",")
		lista.remove(class_element)
		for element in lista:
			elem = element.replace("\n", "")
			toString += elem + ","
		return toString

'''
@Param "line" : linea delle software metrics che vengono splittati e inseriti in una lista 
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
	new_Union = open("union_SM_ASA.csv", "w")
	name_csv_sm = "mining_results_sm_final.csv"
	name_csv_asa = "csv_ASA_final.csv"
	initialize(name_csv_sm, name_csv_asa, new_Union)


if __name__ == '__main__':
	main()