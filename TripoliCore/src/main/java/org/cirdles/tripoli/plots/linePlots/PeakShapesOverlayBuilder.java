package org.cirdles.tripoli.plots.linePlots;

import org.cirdles.tripoli.plots.PlotBuilder;
import org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.dataModels.peakShapes.PeakShapeOutputDataRecord;
import org.cirdles.tripoli.utilities.mathUtilities.MatLab;
import org.cirdles.tripoli.utilities.mathUtilities.SplineBasisModel;
import org.ojalgo.RecoverableCondition;
import org.ojalgo.matrix.decomposition.Cholesky;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;

public class PeakShapesOverlayBuilder extends PlotBuilder {

    private static double measBeamWidthAMU;
    private PeakShapesOverlayRecord peakShapesOverlayRecord;

    public PeakShapesOverlayBuilder() {
    }

    protected PeakShapesOverlayBuilder(int blockID, PeakShapeOutputDataRecord peakShapeOutputDataRecord, String[] title, String xAxisLabel, String yAxisLabel) {
        super(title, xAxisLabel, yAxisLabel, true);

        try {
            peakShapesOverlayRecord = generatePeakShapes(blockID, peakShapeOutputDataRecord);
        } catch (RecoverableCondition e) {
            e.printStackTrace();
        }
    }


    public static PeakShapesOverlayBuilder initializePeakShape(int blockID, PeakShapeOutputDataRecord peakShapeOutputDataRecord, String[] title, String xAxisLabel, String yAxisLabel) {
        return new PeakShapesOverlayBuilder(blockID, peakShapeOutputDataRecord, title, xAxisLabel, yAxisLabel);
    }

    public PeakShapesOverlayRecord generatePeakShapes(int blockID, PeakShapeOutputDataRecord peakShapeOutputDataRecord) throws RecoverableCondition {
        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        double maxBeamIndex;
        double thresholdIntensity;
        int leftBoundary;
        int rightBoundary;

        // Spline basis Basis
        int basisDegree = 3;
        // int orderDiff = 2;
        double beamKnots = Math.ceil(peakShapeOutputDataRecord.beamWindow() / peakShapeOutputDataRecord.deltaMagnetMass()) - (2 * basisDegree);
        int nInterp = 1000;

        double xLower = peakShapeOutputDataRecord.peakCenterMass() - peakShapeOutputDataRecord.beamWindow() / 2;
        double xUpper = peakShapeOutputDataRecord.peakCenterMass() + peakShapeOutputDataRecord.beamWindow() / 2;

        Primitive64Store beamMassInterp = MatLab.linspace(xLower, xUpper, nInterp);
        Primitive64Store Basis = SplineBasisModel.bBase(beamMassInterp, xLower, xUpper, beamKnots, basisDegree);
        double deltaBeamMassInterp = beamMassInterp.get(0, 1) - beamMassInterp.get(0, 0);

        // Calculate integration matrix G, depends on matrix B and peakShapeOutputDataRecord
        int numMagnetMasses = peakShapeOutputDataRecord.magnetMasses().getRowDim();
        double[][] aGMatrix = new double[numMagnetMasses][nInterp];


        for (int iMass = 0; iMass < numMagnetMasses; iMass++) {
            Primitive64Store term1 = MatLab.greaterOrEqual(beamMassInterp, peakShapeOutputDataRecord.collectorLimits().get(iMass, 0));
            Primitive64Store term2 = MatLab.lessOrEqual(beamMassInterp, peakShapeOutputDataRecord.collectorLimits().get(iMass, 1));
            Primitive64Store massesInCollector = MatLab.arrayMultiply(term1, term2);
            Primitive64Store firstMassIndexInside;
            Primitive64Store lastMassIndexInside;
            if (!(0 == MatLab.find(massesInCollector, 1, "first").get(0, 0) && 0 == MatLab.find(massesInCollector, 1, "last").get(0, 0))) {
                firstMassIndexInside = MatLab.find(massesInCollector, 1, "first");
                lastMassIndexInside = MatLab.find(massesInCollector, 1, "last");
                for (int i = (int) (firstMassIndexInside.get(0, 0) + 1); i < (int) (lastMassIndexInside.get(0, 0) + 0); i++) {
                    aGMatrix[iMass][i] = deltaBeamMassInterp;
                }

                aGMatrix[iMass][(int) (firstMassIndexInside.get(0, 0) + 0)] = deltaBeamMassInterp / 2;
                aGMatrix[iMass][(int) (lastMassIndexInside.get(0, 0) + 0)] = deltaBeamMassInterp / 2;

            }
        }

        Primitive64Store gMatrix = storeFactory.rows(aGMatrix);
        // Trim peakShapeOutputDataRecord
        int newDataSet = 0;
        Primitive64Store hasModelBeam = MatLab.any(gMatrix, 2);
        for (int i = 0; i < hasModelBeam.getRowDim(); i++) {
            for (int j = 0; j < hasModelBeam.getColDim(); j++) {
                if (1 == hasModelBeam.get(i, 0)) {
                    newDataSet++;
                }
            }
        }

        if (newDataSet == 0) {
            System.out.println("Error generating plot in block");
            newDataSet = 65;
        }

        double[][] gMatrixTrim = new double[newDataSet][gMatrix.getColDim()];
        int j = 0;
        for (int i = 0; i < gMatrix.getRowDim(); i++) {
            if (0 < hasModelBeam.get(i, 0)) {
                gMatrixTrim[j] = gMatrix.toRawCopy2D()[i];
                j++;
            }
        }

        Primitive64Store trimGMatrix = storeFactory.rows(gMatrixTrim);

        double[][] trimMagnetMasses = new double[newDataSet][peakShapeOutputDataRecord.magnetMasses().getRowDim()];
        int h = 0;

        for (int i = 0; i < peakShapeOutputDataRecord.magnetMasses().getRowDim(); i++) {
            if (0 < hasModelBeam.get(i, 0)) {
                trimMagnetMasses[h] = peakShapeOutputDataRecord.magnetMasses().toRawCopy2D()[i];

                h++;
            }
        }

        double[][] trimPeakIntensity = new double[newDataSet][peakShapeOutputDataRecord.magnetMasses().getRowDim()];
        int k = 0;
        for (int i = 0; i < peakShapeOutputDataRecord.measuredPeakIntensities().getRowDim(); i++) {
            if (0 < hasModelBeam.get(i, 0)) {
                trimPeakIntensity[k] = peakShapeOutputDataRecord.measuredPeakIntensities().toRawCopy2D()[i];
                k++;
            }
        }

        Primitive64Store magnetMasses = storeFactory.rows(trimMagnetMasses);
        Primitive64Store measuredPeakIntensities = storeFactory.rows(trimPeakIntensity);

        double[] intensityData = measuredPeakIntensities.transpose().toRawCopy2D()[0];

        // WLS and NNLS
        MatrixStore<Double> GB = trimGMatrix.multiply(Basis);
        MatrixStore<Double> wData = MatLab.diag(MatLab.rDivide(1, MatLab.max(measuredPeakIntensities, 1)));

        Cholesky<Double> decompChol = Cholesky.PRIMITIVE.make();
        decompChol.decompose(wData);
        MatrixStore<Double> test1OJ = decompChol.getL().multiply(GB);
        MatrixStore<Double> test2OJ = decompChol.getL().multiply(measuredPeakIntensities);
        MatrixStore<Double> beamWNNLS = MatLab.solveNNLS(test1OJ, test2OJ);

        // Determine peak width
        MatrixStore<Double> beamShape = Basis.multiply(beamWNNLS);
        MatrixStore<Double> gBeam = trimGMatrix.multiply(beamShape);
        double MaxBeam = MatLab.normInf(beamShape);
        maxBeamIndex = 0;
        int index = 0;
        for (int i = 0; i < beamShape.getRowDim(); i++) {
            for (int l = 0; l < beamShape.getColDim(); l++) {
                if (beamShape.get(i, l) != MaxBeam) {
                    index++;
                    continue;
                }
                maxBeamIndex = index;
            }
        }

        thresholdIntensity = MaxBeam * (0.01);
        if (maxBeamIndex <= 0 || maxBeamIndex >= 999) {
            measBeamWidthAMU = 0;
            return new PeakShapesOverlayRecord(
                    blockID,
                    measBeamWidthAMU,
                    beamMassInterp.toRawCopy2D()[0],
                    magnetMasses.transpose().toRawCopy2D()[0],
                    beamShape.transpose().toRawCopy2D()[0],
                    gBeam.transpose().toRawCopy2D()[0],
                    intensityData,
                    0,
                    0,
                    title,
                    xAxisLabel,
                    yAxisLabel
            );
        } else {
            double[][] leftPeak = new double[(int) maxBeamIndex][1];
            for (int i = 0; i < maxBeamIndex; i++) {
                leftPeak[i][0] = beamShape.get(i, 0);
            }

            Primitive64Store peakLeft = storeFactory.rows(leftPeak);
            Primitive64Store leftAboveThreshold = MatLab.greaterThan(peakLeft, thresholdIntensity);
            double[][] thresholdLeft1 = new double[leftAboveThreshold.getRowDim() - 1][1];
            double[][] thresholdLeft2 = new double[leftAboveThreshold.getRowDim() - 1][1];
            for (int i = 0; i < leftAboveThreshold.getRowDim() - 1; i++) {
                thresholdLeft1[i][0] = leftAboveThreshold.get(i + 1, 0);
            }

            for (int i = 0; i < leftAboveThreshold.getRowDim() - 1; i++) {
                thresholdLeft2[i][0] = leftAboveThreshold.get(i, 0);
            }
            Primitive64Store leftT1 = storeFactory.rows(thresholdLeft1);
            Primitive64Store leftT2 = storeFactory.rows(thresholdLeft2);

            MatrixStore<Double> leftThresholdChange = leftT1.subtract(leftT2);
            leftBoundary = (int) (MatLab.find(leftThresholdChange, 1, "last").get(0, 0) + 1);


            double[][] rightPeak = new double[beamShape.getRowDim() - (int) maxBeamIndex][1];
            for (int i = 0; i < beamShape.getRowDim() - (int) maxBeamIndex; i++) {
                rightPeak[i][0] = beamShape.get(i + (int) maxBeamIndex, 0);
            }

            Primitive64Store peakRight = storeFactory.rows(rightPeak);
            Primitive64Store rightAboveThreshold = MatLab.greaterThan(peakRight, thresholdIntensity);

            double[][] thresholdRight1 = new double[rightAboveThreshold.getRowDim() - 1][1];
            double[][] thresholdRight2 = new double[rightAboveThreshold.getRowDim() - 1][1];
            for (int i = 0; i < rightAboveThreshold.getRowDim() - 1; i++) {
                thresholdRight1[i][0] = rightAboveThreshold.get(i + 1, 0);
            }

            for (int i = 0; i < rightAboveThreshold.getRowDim() - 1; i++) {
                thresholdRight2[i][0] = rightAboveThreshold.get(i, 0);
            }
            Primitive64Store rightT1 = storeFactory.rows(thresholdRight1);
            Primitive64Store rightT2 = storeFactory.rows(thresholdRight2);
            MatrixStore<Double> rightThresholdChange = rightT2.subtract(rightT1);
            rightBoundary = (int) (MatLab.find(rightThresholdChange, 1, "first").get(0, 0) + maxBeamIndex);

            measBeamWidthAMU = beamMassInterp.get(rightBoundary) - beamMassInterp.get(leftBoundary);
        }

        return new PeakShapesOverlayRecord(
                blockID,
                measBeamWidthAMU,
                beamMassInterp.toRawCopy2D()[0],
                magnetMasses.transpose().toRawCopy2D()[0],
                beamShape.transpose().toRawCopy2D()[0],
                gBeam.transpose().toRawCopy2D()[0],
                intensityData,
                leftBoundary,
                rightBoundary,
                title,
                xAxisLabel,
                yAxisLabel
        );
    }

    public PeakShapesOverlayRecord getPeakShapesOverlayRecord() {
        return peakShapesOverlayRecord;
    }
}
