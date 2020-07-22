# Predicting Vulnerable Code: How far are We?
This is a research experiment from Master's course based in Software Dependability.

The experiment is based on a replication of the state-of-the-art regarding the Prediction of Vulnerable Code.
In fact, the tools with these aims, are called Vulnerability Prediction Models (VPM).

For this reason have been implemented three VPMs techniques based on :
- Software Metrics
- Text Mining
- Automated Static Analysis

Moreover, these technique are combined in order to try to obtain better performance.
So, the combinated techniques are:
- Software Metrics and Text Mining
- Software Metrics and Automated Static Analysis
- Text Mining and Automated Static Analysis
- Software Metrics, Text Mining and Automated Static Analysis

# Machine Learning Classifiers
The following Machine learning techniques have been used :

- Logistic regression (LR)
- Naive Bayes (NB)
- Support Vector Machine (SVM)
- Random Forest (RF)

# Pre-Requirements
- Python 3.4 or newer
- Install PyDriller 1.15
  > *> pip install pydriller*


# Running VPMs
- Clone the GitHub project in your workspace.

## 2. Repository Mining
- Go to **Dataset2** > **RepoMining** and execute *divide_Dataset.py*
- Execute *main_repo_Mining.py*
	> *> python3 main_repo_mining.py*
- At the end of the execution you can see the results in **Dataset2** > **mining_results**

## 3. Software Metrics Execution

To obtain the software metrics dataset it has used *Understand* tool from SciTools.

For each file analyzed it has been extracted the following metrics:

- CountLineCode: Numbers of line contained in the source Code

- CountDeclClass: Number of declared classes in the source code file

- CountDeclFunction: Number of declared functions in the source code file

- CountLineCodeDecl: Number of lines containing declarative source code

- SumEssential: Sum of essential complexity of all nested functions or methods

- SumCyclomaticStrict: Sum of strict cyclomatic complexity of all nested functions or methods

- MaxEssential: Max of essential complexity of all nested functions or methods

- MaxCyclomaticStrict: Maximum strict cyclomatic complexity of nested functions or methods

-	MaxNesting: Maximum nesting level of control constructors

You can see two results datasets inside the **Software_Metrics** folder:
- **mining_results_neg** : contains the software metrics for the negative istances (not vulnerable files)
- **mining_results_pos** : contains the software metrics for the positive istances (vulnerable files)

You can also see the entire dataset file called **mining_results_sm_final**


## 4. Text Mining Execution
- Go to the folder **Dataset2** > **Text_Mining**
- Execute **text_mining.py**
- Execute **dict_generator.py**
- Execute **less_element.py**
- Execute **creator_csv_for_TextMining.py**

You can see the result in **csv_mining_final.csv**


## 5. Automated Static Analysis Execution

The data are extracted by SonarQube with the plugin CNESReport.

Each file analyzed in the dataset is analyzed by SonarQube in respect of 19 rules.

- Go to the **mining_results_asa** folder.

So, the two files resulting from the SonarQube are:
- **RepositoryMining_ASAResults_neg.csv**
- **RepositoryMining_ASAResults_pos.csv**

So, now:
- Execute **ASA_vulnerability_dict_generator.py**
- Execute **rules_dict_generator.py**
- Execute **creator_csv_for_ASA.py**

The resulting file will be in the same folder called **csv_ASA_final.csv**

## 6. Combination
WARNING: Execute first the single techniques steps (3,4,5).

#### Text Mining with Software Metrics
- Go to **dataset2** > **Union** > **Union_TM_SM**
- Execute **Union.py**

You can see the result in the folder **Union_TM_SM** with **union_SM_TM.csv**.

#### Text Mining with Automated Static Analysis
- Go to **dataset2** > **Union** > **Union_TM_ASA**
- Execute **Union_TMwithASA.py**

#### Software Metrics with Automated Static Analysis
- Go to **dataset2** > **Union** > **Union_SM_ASA**
- Execute **Union_SMwithASA.py**

You can see the result in the folder  **Union_SM_ASA** with **union_SM_ASA.csv**.

#### The 3-Combination: Text Mining. Software Metrics and Automated Static Analysis Tool
WARNING: Execute first Text Mining with Automated Static Analysis steps
- Go to **dataset2** > **Union** > **Total_Combination**
- Execute **3Combination.py**

You can see the result in the folder  **Total_Combination** with **3Combination.csv**.  

# Dataset
The following dataset in **Predicting-Vulnerable-Code** > **Dataset2**
is made from **Serena E. Ponta , Henrik Plate, Antonino Sabetta, Michele Bezzi** :
"*A Manually-Curated Dataset of Fixes to Vulnerabilities of
Open-Source Software*"
Available from GitHub at the link : https://github.com/SAP/project-kb/tree/master/MSR2019 .
