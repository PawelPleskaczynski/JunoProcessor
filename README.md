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

Note: on computers with low RAM, I recommend to launch the app with at
least `Xmx2G` argument. Example:

`java -jar -Xmx2G JunoProcessor.jar`

I recommend to have at least 8 GB of RAM for good performance.

##### Processing single image

- Load a JunoCam RAW image by clicking `Open a picture` button. Select your
image via your default file selector.

- Change values of the checkboxes if necessary.

- Click `Process the image` button to start processing the data. Please allow
software up to a minute to process the data.

- Once processing is finished, you'll be notified by a pop-up, and you'll be able
to retrieve final products in the same directory as input data, in a folder named
after the input file's name.

##### Batch processing

- Load a directory with RAW JunoCam images by clicking `Batch processing`
button.

- Change values of the checkboxes if necessary.

- Click `Process the image` button to start processing the data. Please allow
software to work for a couple of minutes. You can see the progress on
progress bar.

- Once processing is finished, you'll be able to retrieve final products
in the same directory as input data, in directories named after the input file's
name.

### FAQ

- Does this software support IMG format? No. Not yet. We intend to
implement PDS native IMG format. For now, only PNG format is viable. We
recommend IMG2PNG software to convert from PDS IMG format to PNG.
- Why are edges of Jupiter in images jagged? This is a normal artifact
due to build of JunoCam system. We are not aware of any methods to
reduce this any further than our software does, without utilizing
non-linear geometric reduction.

For any further inquiry, please contact us at:

- pawelpleskaczynski@gmail.com - Paweł Pleskaczyński

- karolmasztalerz@gim-nt.pl - Karol Masztalerz
