'''
This script add the class type [pos || neg] for each element inside the metrics dataset.
@Param filename: name of the dataset.
@param class_type: value of the class type.
'''
def initialize(filename, class_type):
	csv_file=open(filename,"r")
	result_file=open("class"+filename,"w+")
	result_file.write(csv_file.readline().rstrip()+",class"+"\n")
	for line in csv_file.read().splitlines():
		result_file.write(line + "," + class_type +"\n")
	csv_file.close()
	result_file.close()

def main():
	print("Insert the name of the dataset [with extension] ")
	filename = input()
	print("Define the class: [pos || neg]")
	class_type = input()
    initialize(filename, class_type)
if __name__ == '__main__':
    main()
