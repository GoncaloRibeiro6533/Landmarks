package pt.isel.cn;

public class MonumentDomain {
    public Float confidence;
    public String name;
    public LocationDomain location;

    public MonumentDomain() {}

    public MonumentDomain(float confidence, String name, LocationDomain location) {
        this.confidence = confidence;
        this.name = name;
        this.location = location;
    }
}



/*
        "monuments": [
            {
                "name": "Monument Name",
                "location": {
                    "latitude": 12.34,
                    "longitude": 56.78
                },
                "confidence": 0.95
            }
        ]
    }

 */