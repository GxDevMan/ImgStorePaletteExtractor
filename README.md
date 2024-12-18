# Image Store Palette Extractor Project

The project was made to explore design patterns with java. This project creates databases which images may be
contained in. This app also allows for the extraction of palettes based on strategies like Kmeans or Mean Shift.

Saving of Images and other underlying data is handled by hibernate.
Hibernate is configured to use the sql Lite dialect.

## Design Patterns Used

- Mediator
- Observer
- Factory
- Singleton
- Strategy

## Features

- Palette Extraction
  - Kmeans
  - Mean Shift
  - Histogram
  - Region Based
  - Gaussian Mixture Model
  - Spectral Clustering


- Saving of images
- Tagging of Images
- Searching of Images via tag or title or both
- Regex Search
