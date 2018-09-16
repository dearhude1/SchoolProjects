package tools;

public class Pair<V1, V2 ,V3>
{
    public V1 v1;

    public V2 v2;
    
    public V3 v3;

    public Pair(V1 v1, V2 v2, V3 v3)
    {
	this.v1 = v1;
	this.v2 = v2;
	this.v3 = v3;
    }

    public int hashCode()
    {
	return v1.hashCode() | v2.hashCode() | v3.hashCode();
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object obj)
    {
	if (obj == null)
	    return false;
	else if (!(obj instanceof Pair))
	{
	    return false;
	}
	Pair<V1,V2,V3> p = (Pair<V1,V2,V3>) obj;
	return p.v1.equals(v1) && p.v2.equals(v2) && p.v3.equals(v3);
    }

    public String toString()
    {
	return v1.toString() + ":" + v2.toString()+ ":" + v3.toString();
    }
   
}
