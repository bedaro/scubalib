# scubalib
Java library for SCUBA diving-related computations.

## About

This library began as part of an Android gas blending app. It provides a
mature object model for managing gas blending, modeling with both the ideal
gas law and the Van der Waals equation of state. Additionally, the Cylinder
class can track hydrostatic testing/visual inspection dates. It can determine
whether or not one of those tests has expired or is about to expire. Cylinder
contains a few other fields, getters, and setters which makes it useful for
mapping to a database table.

There is also logic to handle values and computations in both Imperial and
Metric unit systems. Values can be converted between the systems as needed.

This library does not handle blending computations themselves; that code
resides in my Android app [Gas Mixer](https://github.com/bedaro/gasmixer).
There is a lot of logic specific to blending methods/order and a dependency
on a matrix math library to make it work. This library's main job is to
convert a blending solution (how much oxygen, helium, and topup mix needs to
be added) into the pressure gauge readings one can expect as each constituent
goes into the fill cylinder.

I did some work years ago to extend this library for decompression planning,
but abandoned the project. Most of that code is relegated to a separate branch
and should not be considered stable.

## Getting Started

The most important classes for blending are Mix, Cylinder, and GasSupply.
A GasSupply represents a Cylinder which contains a gas Mix at a certain
pressure. Its main methods `addGas(mix, amount)`, `topup(mix, final_pressure)`
and `drainToGasAmount(amt)` modify the contents. The methods `getPressure()`
and `getGasAmount()` can provide details on the results. For more, see the
Javadocs and unit tests.

## Contact

You can reach me by email at <ben@benroberts.me> with questions or
contributions.
