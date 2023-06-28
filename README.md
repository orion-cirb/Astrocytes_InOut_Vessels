# Astrocytes_InOut_Vessels

* **Developed for:** Katia
* **Team:** Cohen-Salmon
* **Date:** June 2023
* **Software:** Fiji

### Images description

3D images taken with a 40x objective.

3 channels:
  1. *CSU 488:* Aldh-GFP/BacTRAP astrocytes
  2. *CSU 561:* Iba1 microglia
  3. *CSU 642:* IB4 vessels
### Plugin description

* Detect vessels with LoG + thresholding
* Detect microglia with median filtering + thresholding and fill them in black in vessels mask
* Detect astrocytes with median filtering + thresholding
* Find astrocytic objects in/out dilated vessels
* Measure vessels volume + astrocytic objects volume in/out vessels
* If ROI(s) provided, remove from the analysis vessels and RNA dots that are inside

### Dependencies

* **3DImageSuite** Fiji plugin
* **CLIJ** Fiji plugin

### Version history

Version 1 released on June 28, 2023.
