public class GetCurrentConsumptionMessage extends MessageType {
    public GetCurrentConsumptionMessage(Agent sender) {
        super(sender);
    }
}

public class GetForecastConsumptionMessage extends MessageType {
    public final double timestampStart;
    public final double timestampEnd;

    public GetForecastConsumptionMessage(Agent sender, double timestampStart, double timestampEnd) {
        super(sender);
        this.timestampStart = timestampStart;
        this.timestampEnd = timestampEnd;
    }
}

public class DataMessageFromPersonEnergy extends MessageType {
    public final double consumption;

    public DataMessageFromPersonEnergy(Agent sender, double consumption) {
        super(sender);
        this.consumption = consumption;
    }
}

public class DataMessageFromPersonEnergyForecast extends MessageType {
    public final double[] forecast;
    public final double timestampStart;
    public final double timestampEnd;

    public DataMessageFromPersonEnergyForecast(Agent sender, double timestampStart, double timestampEnd, double[] forecast) {
        super(sender);
        this.timestampStart = timestampStart;
        this.timestampEnd = timestampEnd;
        this.forecast = forecast;
    }
}