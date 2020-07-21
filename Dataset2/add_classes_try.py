def initialize(filename):
	csv_file=open(filename,"r")
	result_file=open("class"+filename,"w+")
	result_file.write(csv_file.readline().rstrip()+",class"+"\n")
	for line in csv_file.read().splitlines():
		result_file.write(line+",neg"+"\n")
	csv_file.close()
	result_file.close()

def main():
    initialize("RepositoryMining19-35.csv")
if __name__ == '__main__':
    main()
