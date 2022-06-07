# DeNovoGUI #

  * [Introduction](#introduction)
  * [Command Line](#command-line)
  * [Issues, Questions and Bug Reports](#Issues-questions-and-bug-reports)
  
  * [Bioinformatics for Proteomics Tutorial](http://compomics.com/bioinformatics-for-proteomics/)

---

## DeNovoGUI Publication:
  * Muth T, Weilnböck L, Rapp E, Huber CG, Martens L, Vaudel M, Barsnes H: _DeNovoGUI: an open source graphical user interface for de novo sequencing of tandem mass spectra_. [PMID 24295440](http://www.ncbi.nlm.nih.gov/pubmed/24295440).
  * If you use DeNovoGUI as part of a publication, please include this reference.

---

|   |   |   |
| :------------------------- | :--------------- | :--: |
| [![download](https://github.com/compomics/denovogui/wiki/images/download_denovogui_button.png)](https://genesis.ugent.be/maven2/com/compomics/denovogui/DeNovoGUI/1.16.7/DeNovoGUI-1.16.7-windows.zip) | *v1.16.7 - Windows* | [ReleaseNotes](https://github.com/compomics/denovogui/wiki/ReleaseNotes) |
| [![download](https://github.com/compomics/denovogui/wiki/images/download_denovogui_button_mac_linux.png)](https://genesis.ugent.be/maven2/com/compomics/denovogui/DeNovoGUI/1.16.7/DeNovoGUI-1.16.7-mac_and_linux.tar.gz) | *v1.16.7 - Mac and Linux* |[ReleaseNotes](https://github.com/compomics/denovogui/wiki/ReleaseNotes) |

---

|   |   |
| :--: | :--: |
| [![](https://github.com/compomics/denovogui/wiki/images/DeNovoGUI_small.png)](https://github.com/compomics/denovogui/wiki/images/DeNovoGUI.png) | [![](https://github.com/compomics/denovogui/wiki/images/DeNovoGUI_2_small.png)](https://github.com/compomics/denovogui/wiki/images/DeNovoGUI_2.png) |

---

## Introduction ##

**DeNovoGUI** provides a user-friendly open-source graphical user interface for running the _de novo_ sequencing algorithms [Novor](http://rapidnovor.com), [DirecTag](http://fenchurch.mc.vanderbilt.edu/bumbershoot/directag/), [PepNovo+](http://proteomics.ucsd.edu/Software/PepNovo.html) and [pNovo+](http://pfind.ict.ac.cn/software/pNovo/) (beta) on Windows, Mac and Linux.

To start using DeNovoGUI, unzip the downloaded file, and double-click the `DeNovoGUI-X.Y.Z.jar file`. No additional installation required!

[Go to top of page](#denovogui)

---

## Command Line ##

DeNovoGUI can also be used via the command line, see [DeNovoCLI](https://github.com/compomics/denovogui/wiki/DeNovoCLI) for details.

[Go to top of page](#denovogui)

---


## Issues, Questions and Bug Reports ##

### Code of Conduct

As part of our efforts toward delivering open and inclusive science, we follow the [Contributor Convenant](https://www.contributor-covenant.org/) [Code of Conduct for Open Source Projects](docs/CODE_OF_CONDUCT.md).

### Issue Tracker

Despite our efforts at enforcing good practices in our work, like every software, DeNovoGUI might crash, fail to cover specific use cases, not perform as well as expected. We apologize for any inconvenience and will try to fix things to the best of our capabilities. We welcome bug reports, suggestions of improvements, and contributions _via_ our [issue tracker](https://github.com/compomics/denovogui/issues). 

### Troubleshooting

Note that PepNovo+ is not supported on Linux 32 bit, pNovo+ is only supported on Windows, and that DirectTag is currently not supported on Mac (support is in development).

If you get problems running PepNovo+ on Linux (or OSX), please try the following: (1) install dos2unix, (2) run the following two commands inside the resources/PepNovo folder: `dos2unix Models/*.*` and `dos2unix Models/*/*.*`.

For problems related to Java, memory or startup issues, please see: [Java Troubleshooting](https://github.com/compomics/compomics-utilities/wiki/JavaTroubleShooting).

For other questions or suggestions please contact the developers of DeNovoGUI by setting up an [Issue](https://github.com/compomics/denovogui/issues) with your comment, or by sending an e-mail to the [DeNovoGUI Google Group](http://groups.google.com/group/denovogui).

[Go to top of page](#denovogui)

---
