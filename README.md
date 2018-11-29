# JunoProcessor
## JunoCam Data reduction software.

Goal of this software is to reduce RAW JunoCam data, from RAW push-broom
images, into sets of demosaiced, aligned multispectral R G B channel
images.

### Installation

- Make sure you have Java installed on your computer (and JavaFX libraries.
  These aren't included in `openjdk` packages for Linux).
- Download `JunoProcessor.jar` file
- Launch the file

### Usage

Open the software using `JunoProcessor.jar` executable, located in
[Releases](https://github.com/PawelPleskaczynski/JunoProcessor/releases) tab.

To load a JunoCam RAW image, click `Open Picture` button. Select your
image via your default file selector.

Click `Process the image` button to start processing the data. Please allow
software up to a minute to process the data.

Once processing is finished, you'll be notified by a pop-up, and you'll be able
to retrieve final products in the same directory as input data, in a folder named
after the input file's name.

[//]: # (Batch processing)

### FAQ

- Does this software support IMG format? No. Not yet. We intend to
implement PDS native IMG format. For now, only PNG format is viable. We
recommend IMG2PNG software to convert from PDS IMG format to PNG.
- Why are edges of Jupiter in images jagged? This is a normal artifact
due to build of JunoCam system. We are not aware of any methods to
reduce this any further than our software does, without utilizing
non-linear geometric reduction.
[//]: # (- Why is the color balance so red after
the processing? This is admittedly a bug in software for which we can
not find a solution as of yet. It can be readily fixed by applying
color calibration methods in further data processing. This bug is
however minor and does not affect data quality.)

For any further inquiry, please contact us at:

- pawelpleskaczynski@gmail.com - Paweł Pleskaczyński

- karolmasztalerz@gim-nt.pl - Karol Masztalerz
