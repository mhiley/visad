/* Generated by Together */

package visad.data.in;

abstract public class VirtualDataSource
    extends VirtualDataFilter
{
    protected VirtualDataSource(VirtualDataSink downstream)
    {
	super(downstream);
    }

    public abstract boolean open(String spec);
}
